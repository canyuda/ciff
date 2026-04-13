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
    INTERNAL_ERROR(1999, "Internal server error");

    private final int code;
    private final String message;
}
