package com.ciff.common.http;

import lombok.Getter;

@Getter
public class LlmApiException extends RuntimeException {

    public enum ErrorType {
        TIMEOUT,
        AUTH_FAILED,
        RATE_LIMITED,
        UNKNOWN
    }

    private final ErrorType errorType;
    private final int statusCode;

    public LlmApiException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.statusCode = 0;
    }

    public LlmApiException(ErrorType errorType, int statusCode, String message) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    public LlmApiException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = 0;
    }

    public static LlmApiException timeout(String url, long elapsedMs) {
        return new LlmApiException(ErrorType.TIMEOUT,
                "LLM request timeout: " + url + ", elapsed: " + elapsedMs + "ms");
    }

    public static LlmApiException authFailed(String url, String body) {
        return new LlmApiException(ErrorType.AUTH_FAILED, 401,
                "LLM auth failed: " + url + ", response: " + truncate(body));
    }

    public static LlmApiException rateLimited(String url, String body) {
        return new LlmApiException(ErrorType.RATE_LIMITED, 429,
                "LLM rate limited: " + url + ", response: " + truncate(body));
    }

    public static LlmApiException httpError(String url, int statusCode, String body) {
        return new LlmApiException(ErrorType.UNKNOWN, statusCode,
                "LLM request failed: " + url + ", status: " + statusCode + ", response: " + truncate(body));
    }

    public static LlmApiException unknown(String url, Throwable cause) {
        return new LlmApiException(ErrorType.UNKNOWN,
                "LLM request error: " + url, cause);
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}
