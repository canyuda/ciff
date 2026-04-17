package com.ciff.common.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Extracts X-User-Id header into UserContext.
 * Temporary solution before JWT auth (Phase 6).
 */
@Component
public class UserIdInterceptor implements HandlerInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader(HEADER_USER_ID);
        if (userIdStr != null && !userIdStr.isBlank()) {
            UserContext.setUserId(Long.parseLong(userIdStr));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
