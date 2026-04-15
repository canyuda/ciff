package com.ciff.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 1000-1999: common
    BAD_REQUEST(1000, "Invalid request"),
    PARAM_VALIDATION_FAILED(1001, "Parameter validation failed"),
    UNAUTHORIZED(1002, "Unauthorized"),
    FORBIDDEN(1003, "Access denied"),
    NOT_FOUND(1004, "Resource not found"),
    TOO_MANY_REQUESTS(1005, "Too many requests"),
    LLM_UNAVAILABLE(1006, "AI service temporarily unavailable"),
    LLM_AUTH_FAILED(1007, "AI service authentication failed"),
    LLM_RATE_LIMITED(1008, "AI service rate limit exceeded"),
    INTERNAL_ERROR(1999, "Internal server error");

    private final int code;
    private final String message;
}
