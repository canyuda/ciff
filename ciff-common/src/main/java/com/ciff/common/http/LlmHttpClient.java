package com.ciff.common.http;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * LLM HTTP 客户端，支持同步 POST 和 SSE 流式调用。
 *
 * <p>四级超时机制：
 * <ol>
 *   <li>TCP 连接超时（5s）— 建立连接的最长等待</li>
 *   <li>读取超时（sync 60s / stream 120s）— 整体请求最长等待</li>
 *   <li>首 Token 超时（默认 30s）— SSE 流首个数据到达的最长等待</li>
 *   <li>Token 间隔超时（默认 15s）— SSE 流两个事件之间的最长间隔</li>
 * </ol>
 */
@Component
public class LlmHttpClient {

    private static final Logger log = LoggerFactory.getLogger(LlmHttpClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration SYNC_READ_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration STREAM_READ_TIMEOUT = Duration.ofSeconds(120);
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient syncClient;
    private final WebClient streamClient;
    private final Duration firstTokenTimeout;
    private final Duration tokenIntervalTimeout;

    public LlmHttpClient(
            @Value("${ciff.llm.timeout.first-token:30s}") Duration firstTokenTimeout,
            @Value("${ciff.llm.timeout.token-interval:15s}") Duration tokenIntervalTimeout) {
        this.syncClient = buildClient(CONNECT_TIMEOUT, SYNC_READ_TIMEOUT);
        this.streamClient = buildClient(CONNECT_TIMEOUT, STREAM_READ_TIMEOUT);
        this.firstTokenTimeout = firstTokenTimeout;
        this.tokenIntervalTimeout = tokenIntervalTimeout;
    }

    /**
     * 同步 POST 请求，阻塞等待完整响应。
     */
    public String post(String url, Map<String, String> headers, String body) {
        long start = System.currentTimeMillis();

        return syncClient.post()
                .uri(url)
                .headers(h -> applyHeaders(h, headers))
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).flatMap(errorBody -> {
                            long elapsed = System.currentTimeMillis() - start;
                            int status = resp.statusCode().value();
                            log.warn("LLM POST {} - status: {}, elapsed: {}ms", maskApiKey(url), status, elapsed);
                            return Mono.error(mapError(status, errorBody, url));
                        }))
                .bodyToMono(String.class)
                .doOnNext(result -> {
                    long elapsed = System.currentTimeMillis() - start;
                    log.info("LLM POST {} - status: 200, elapsed: {}ms", maskApiKey(url), elapsed);
                })
                .block();
    }

    /**
     * SSE 流式 POST 请求。每个 SSE event 回调 callback。
     *
     * <p>在 stream() 方法中实现首 Token 和 Token 间隔超时检测：
     * 通过 Flux 的 timeout 操作符，以动态计算的超时值检测数据到达情况。</p>
     */
    public void stream(String url, Map<String, String> headers, String body, Consumer<String> callback) {
        long start = System.currentTimeMillis();
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);
        AtomicLong lastEventTime = new AtomicLong(System.currentTimeMillis());

        try {
            streamClient.post()
                    .uri(url)
                    .headers(h -> applyHeaders(h, headers))
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(errorBody -> {
                                int status = resp.statusCode().value();
                                log.warn("LLM STREAM {} - status: {}, elapsed: {}ms",
                                        maskApiKey(url), status, System.currentTimeMillis() - start);
                                return Mono.error(mapError(status, errorBody, url));
                            }))
                    .bodyToFlux(SSE_TYPE)
                    .mapNotNull(ServerSentEvent::data)
                    .takeUntil("[DONE]"::equals)
                    .timeout(firstTokenTimeout, Mono.error(
                            new TimeoutException("首 Token 超时: " + firstTokenTimeout.toSeconds() + "s")))
                    .doOnNext(data -> {
                        // 首 Token 到达后，后续事件使用 tokenIntervalTimeout
                        if (firstTokenReceived.compareAndSet(false, true)) {
                            log.info("LLM STREAM {} - first token received in: {}ms",
                                    maskApiKey(url), System.currentTimeMillis() - start);
                        }
                        lastEventTime.set(System.currentTimeMillis());

                        if (!"[DONE]".equals(data)) {
                            callback.accept(data);
                        }
                    })
                    // 首个 token 之后，切换为 token 间隔超时
                    .skip(1)
                    .timeout(tokenIntervalTimeout)
                    .blockLast();
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException te) {
                long elapsed = System.currentTimeMillis() - start;
                throw LlmApiException.timeout(url, elapsed);
            }
            if (e instanceof TimeoutException) {
                long elapsed = System.currentTimeMillis() - start;
                throw LlmApiException.timeout(url, elapsed);
            }
            // reactor 的 TimeoutException 会被包装，需要检查消息
            if (e.getClass().getName().contains("Timeout")) {
                long elapsed = System.currentTimeMillis() - start;
                throw LlmApiException.timeout(url, elapsed);
            }
            throw e;
        }

        log.info("LLM STREAM {} - completed in: {}ms", maskApiKey(url), System.currentTimeMillis() - start);
    }

    private WebClient buildClient(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(readTimeout);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    private void applyHeaders(HttpHeaders httpHeaders, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }
        if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
    }

    private LlmApiException mapError(int statusCode, String body, String url) {
        if (statusCode == 401) {
            return LlmApiException.authFailed(url, body);
        }
        if (statusCode == 429) {
            return LlmApiException.rateLimited(url, body);
        }
        return LlmApiException.httpError(url, statusCode, body);
    }

    private String maskApiKey(String url) {
        return url.replaceAll("([?&])(api_key|key|token|secret)=([^&]+)", "$1$2=***");
    }
}
