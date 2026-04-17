package com.ciff.common.exception;

import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.Result;
import com.ciff.common.http.LlmApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException e) {
        log.warn("Business error: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(LlmApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleLlmApiException(LlmApiException e) {
        ErrorCode errorCode = switch (e.getErrorType()) {
            case AUTH_FAILED -> ErrorCode.LLM_AUTH_FAILED;
            case RATE_LIMITED -> ErrorCode.LLM_RATE_LIMITED;
            default -> ErrorCode.LLM_UNAVAILABLE;
        };
        log.error("LLM API error: type={}, message={}", e.getErrorType(), e.getMessage());
        return Result.fail(errorCode.getCode(), errorCode.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return Result.fail(ErrorCode.PARAM_VALIDATION_FAILED.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Request body parse error: {}", e.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体解析失败: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
