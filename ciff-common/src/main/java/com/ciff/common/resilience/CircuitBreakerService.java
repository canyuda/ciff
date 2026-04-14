package com.ciff.common.resilience;

import com.ciff.common.http.LlmApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerService {

    private final CircuitBreakerProperties properties;
    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    /**
     * Get or create a circuit breaker instance for the given provider.
     */
    public CircuitBreaker getOrCreate(String providerName) {
        return breakers.computeIfAbsent(providerName, this::createCircuitBreaker);
    }

    /**
     * Execute action with circuit breaker protection and retry logic.
     *
     * Circuit breaker wraps the entire retry loop:
     * - If CB is OPEN, fail fast without attempting
     * - If CB is CLOSED/HALF_OPEN, attempt with retries
     * - Final result (success or exhausted retries) is recorded by CB
     * - AUTH_FAILED is NOT recorded as failure (ignored by recordException predicate)
     *
     * Retry rules by error type:
     * - TIMEOUT:        retry up to 2 times, fixed 1s interval (total 3 attempts)
     * - RATE_LIMITED:   retry up to 2 times, exponential backoff 2s -> 4s
     * - AUTH_FAILED:    no retry, fail immediately
     * - UNKNOWN:        no retry
     */
    public <T> T execute(String providerName, Supplier<T> action) {
        CircuitBreaker cb = getOrCreate(providerName);
        return cb.executeSupplier(() -> executeWithRetry(providerName, action));
    }

    private CircuitBreaker createCircuitBreaker(String providerName) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(properties.getSlidingWindowSize())
                .failureRateThreshold(properties.getFailureRateThreshold())
                .waitDurationInOpenState(properties.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState())
                // AUTH_FAILED does not count as failure — config error, not transient error
                .recordException(throwable ->
                        throwable instanceof LlmApiException ex
                                && ex.getErrorType() != LlmApiException.ErrorType.AUTH_FAILED)
                .build();

        CircuitBreaker cb = CircuitBreaker.of(providerName, config);
        log.info("Created circuit breaker for provider: {}", providerName);
        return cb;
    }

    private <T> T executeWithRetry(String providerName, Supplier<T> action) {
        int maxRetries = 2;
        LlmApiException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (LlmApiException e) {
                lastException = e;
                if (!isRetryable(e.getErrorType()) || attempt >= maxRetries) {
                    throw e;
                }
                long interval = getRetryInterval(e.getErrorType(), attempt);
                log.warn("[{}] LLM call failed ({}) on attempt {}/{}, retrying in {}ms",
                        providerName, e.getErrorType(), attempt + 1, maxRetries + 1, interval);
                sleep(interval);
            }
        }
        throw lastException;
    }

    private boolean isRetryable(LlmApiException.ErrorType type) {
        return type == LlmApiException.ErrorType.TIMEOUT
                || type == LlmApiException.ErrorType.RATE_LIMITED;
    }

    // Exponential backoff for rate limit: 2s, 4s; Fixed 1s for timeout
    private long getRetryInterval(LlmApiException.ErrorType type, int attempt) {
        return switch (type) {
            case TIMEOUT -> 1000L;
            case RATE_LIMITED -> (long) (2000 * Math.pow(2, attempt)); // 2s, 4s
            default -> 0L;
        };
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmApiException(LlmApiException.ErrorType.UNKNOWN,
                    "Retry sleep interrupted", e);
        }
    }
}