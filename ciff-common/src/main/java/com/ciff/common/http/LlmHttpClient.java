package com.ciff.common.http;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.function.Consumer;

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

    public LlmHttpClient() {
        this.syncClient = buildClient(CONNECT_TIMEOUT, SYNC_READ_TIMEOUT);
        this.streamClient = buildClient(CONNECT_TIMEOUT, STREAM_READ_TIMEOUT);
    }

    /**
     * Synchronous POST request, blocks until response is received.
     *
     * Flux chain:
     *   post().uri(url).bodyValue(body)  -- build POST request
     *   .retrieve()                      -- send request
     *   .onStatus(isError, ...)          -- if 4xx/5xx, read error body and throw LlmApiException
     *   .bodyToMono(String)              -- on success, read entire response body as String
     *   .doOnNext(log)                   -- log success (status 200, elapsed time)
     *   .block()                         -- block current thread, wait for complete response
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
     * Streaming SSE POST request. Calls callback for each SSE data event.
     * Must be called from llmExecutor thread (blocking I/O).
     *
     * Flux chain (operators are assembled top-down, data flows when blockLast() triggers subscription):
     *   post().uri(url).bodyValue(body)  -- build POST request
     *   .retrieve()                      -- send request
     *   .onStatus(isError, ...)          -- if 4xx/5xx, throw LlmApiException
     *   .bodyToFlux(SSE_TYPE)            -- 1. stream SSE events, each event is ServerSentEvent<String>
     *   .mapNotNull(getData)             -- 2. extract data field, filter out nulls (keep-alive events)
     *   .takeUntil("[DONE]")             -- 3. terminate stream when [DONE] arrives ([DONE] is the last element)
     *   .doOnNext(callback)              -- 4. for each element: skip [DONE], pass the rest to callback
     *   .doOnComplete(log)               -- 5. log when stream completes
     *   .blockLast()                     -- 6. block current thread until stream ends
     */
    public void stream(String url, Map<String, String> headers, String body, Consumer<String> callback) {
        long start = System.currentTimeMillis();

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
                .doOnNext(data -> {
                    if (!"[DONE]".equals(data)) {
                        callback.accept(data);
                    }
                })
                .doOnComplete(() ->
                        log.info("LLM STREAM {} - completed in: {}ms",
                                maskApiKey(url), System.currentTimeMillis() - start))
                .blockLast();
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