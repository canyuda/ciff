package com.ciff.common.aspect;

import com.ciff.common.annotation.IgnoreLogRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Set<Class<?>> SKIP_PARAM_TYPES = Set.of(
            HttpServletRequest.class,
            HttpServletResponse.class
    );

    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie");

    private final ObjectMapper objectMapper;

    @Value("${ciff.logging.max-payload-length:2000}")
    private int maxPayloadLength;

    public RequestLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.GetMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.PostMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        if (shouldSkip(signature)) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = getCurrentRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String threadName = Thread.currentThread().getName();
        String headers = serializeHeaders(request);
        String params = serializeParams(joinPoint);

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        String response = serializeResult(result);

        Logger logger = LoggerFactory.getLogger(signature.getDeclaringType());
        logger.info("[HTTP] {} {} | thread: {} | headers: {} | params: {} | response: {} | duration: {}ms",
                method, uri, threadName, headers, params, response, duration);

        return result;
    }

    private boolean shouldSkip(MethodSignature signature) {
        if (signature.getMethod().isAnnotationPresent(IgnoreLogRequest.class)) {
            return true;
        }
        return signature.getDeclaringType().isAnnotationPresent(IgnoreLogRequest.class);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest();
    }

    private String serializeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = SENSITIVE_HEADERS.contains(name.toLowerCase())
                    ? "******"
                    : request.getHeader(name);
            headers.put(name, value);
        }
        return toJson(headers);
    }

    private String serializeParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "";
        }
        Map<String, Object> params = new LinkedHashMap<>();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || SKIP_PARAM_TYPES.contains(arg.getClass())
                    || arg instanceof MultipartFile) {
                continue;
            }
            String name = (paramNames != null && paramNames.length > i) ? paramNames[i] : ("arg" + i);
            params.put(name, arg);
        }
        return toJson(params);
    }

    private String serializeResult(Object result) {
        if (result == null) {
            return "";
        }
        return toJson(result);
    }

    private String toJson(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            if (json.length() <= maxPayloadLength) {
                return json;
            }
            return json.substring(0, maxPayloadLength) + "...[truncated]";
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
