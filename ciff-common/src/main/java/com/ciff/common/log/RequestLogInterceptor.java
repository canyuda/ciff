package com.ciff.common.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);
    private static final String TRACE_ID = "traceId";
    private static final String START_TIME = "requestStartTime";
    private static final long SLOW_THRESHOLD_MS = 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = generateTraceId();
        MDC.put(TRACE_ID, traceId);
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            long elapsed = System.currentTimeMillis() - (long) request.getAttribute(START_TIME);
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();

            if (elapsed > SLOW_THRESHOLD_MS) {
                log.warn("[HTTP] {} {} | status: {} | elapsed: {}ms (SLOW)", method, path, status, elapsed);
            } else {
                log.info("[HTTP] {} {} | status: {} | elapsed: {}ms", method, path, status, elapsed);
            }

            if (ex != null) {
                log.error("[HTTP] {} {} | unhandled exception", method, path, ex);
            }
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}