package com.ciff.common.http;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ciff.common.resilience.CircuitBreakerProperties;
import com.ciff.common.resilience.CircuitBreakerService;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmHttpClientTest {

    private MockWebServer server;
    private LlmHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        CircuitBreakerService cbService = new CircuitBreakerService(new CircuitBreakerProperties());
        client = new LlmHttpClient(Duration.ofSeconds(2), Duration.ofSeconds(1), cbService);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void post_shouldReturnResponseBody() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody("{\"choices\":[]}")
                .setHeader("Content-Type", "application/json"));

        String url = server.url("/v1/chat/completions").toString();
        String result = client.post(url, Map.of("Authorization", "Bearer sk-test"), "{}");

        assertEquals("{\"choices\":[]}", result);

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("Bearer sk-test", request.getHeader("Authorization"));
    }

    @Test
    void post_when401_shouldThrowAuthFailed() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));

        String url = server.url("/v1/chat/completions").toString();

        LlmApiException ex = assertThrows(LlmApiException.class,
                () -> client.post(url, Map.of(), "{}"));

        assertEquals(LlmApiException.ErrorType.AUTH_FAILED, ex.getErrorType());
    }

    @Test
    void post_when429_shouldThrowRateLimited() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("rate limited"));

        String url = server.url("/v1/chat/completions").toString();

        LlmApiException ex = assertThrows(LlmApiException.class,
                () -> client.post(url, Map.of(), "{}"));

        assertEquals(LlmApiException.ErrorType.RATE_LIMITED, ex.getErrorType());
    }

    @Test
    void post_when500_shouldThrowUnknown() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("internal error"));

        String url = server.url("/v1/chat/completions").toString();

        LlmApiException ex = assertThrows(LlmApiException.class,
                () -> client.post(url, Map.of(), "{}"));

        assertEquals(LlmApiException.ErrorType.UNKNOWN, ex.getErrorType());
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    void stream_shouldReceiveAllEvents() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"content\":\"hello\"}\n\ndata: {\"content\":\"world\"}\n\ndata: [DONE]\n\n"));

        StringBuilder collected = new StringBuilder();
        String url = server.url("/v1/chat/completions").toString();

        client.stream(url, Map.of("Authorization", "Bearer sk-test"), "{}", collected::append);

        assertEquals("{\"content\":\"hello\"}{\"content\":\"world\"}", collected.toString());
    }

    @Test
    void stream_whenFirstTokenTimeout_shouldThrowTimeout() {
        // 模拟首 Token 超时：延迟发送数据，超过 firstTokenTimeout
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: late\n\ndata: [DONE]\n\n")
                .setBodyDelay(1, java.util.concurrent.TimeUnit.SECONDS));

        // 使用很短的超时便于测试
        CircuitBreakerService cbService = new CircuitBreakerService(new CircuitBreakerProperties());
        LlmHttpClient shortTimeoutClient = new LlmHttpClient(Duration.ofMillis(200), Duration.ofMillis(100), cbService);
        String url = server.url("/v1/chat/completions").toString();

        LlmApiException ex = assertThrows(LlmApiException.class,
                () -> shortTimeoutClient.stream(url, Map.of(), "{}", data -> {}));

        assertEquals(LlmApiException.ErrorType.TIMEOUT, ex.getErrorType());
    }

    @Test
    void stream_whenHttpError_shouldThrowAppropriateException() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));

        String url = server.url("/v1/chat/completions").toString();

        LlmApiException ex = assertThrows(LlmApiException.class,
                () -> client.stream(url, Map.of(), "{}", data -> {}));

        assertEquals(LlmApiException.ErrorType.AUTH_FAILED, ex.getErrorType());
    }

    @Test
    void post_withNullHeaders_shouldUseDefaultContentType() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("ok"));

        client.post(server.url("/test").toString(), null, "{}");

        RecordedRequest request = server.takeRequest();
        assertEquals("application/json", request.getHeader("Content-Type"));
    }
}
