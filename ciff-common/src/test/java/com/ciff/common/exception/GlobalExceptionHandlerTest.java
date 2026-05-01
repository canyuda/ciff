package com.ciff.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotLoginException_tokenTimeout_returnsAuthExpired() {
        NotLoginException e = new NotLoginException("token expired", "login", NotLoginException.TOKEN_TIMEOUT);

        Result<Void> result = handler.handleNotLoginException(e);

        assertThat(result.getCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED.getCode());
        assertThat(result.getMessage()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED.getMessage());
    }

    @Test
    void handleNotLoginException_invalidToken_returnsUnauthorized() {
        NotLoginException e = new NotLoginException("invalid token", "login", NotLoginException.INVALID_TOKEN);

        Result<Void> result = handler.handleNotLoginException(e);

        assertThat(result.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        assertThat(result.getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void handleNotLoginException_noToken_returnsUnauthorized() {
        NotLoginException e = new NotLoginException("not token", "login", NotLoginException.NOT_TOKEN);

        Result<Void> result = handler.handleNotLoginException(e);

        assertThat(result.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    void handleNotLoginException_kickOut_returnsUnauthorized() {
        NotLoginException e = new NotLoginException("kicked out", "login", NotLoginException.KICK_OUT);

        Result<Void> result = handler.handleNotLoginException(e);

        assertThat(result.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }
}
