package com.popcorn.demo.presentation.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        log.info("Incoming request: {} {} traceId={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("traceId"));
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        Object startTime = request.getAttribute(START_TIME_ATTR);
        long elapsedMs = startTime instanceof Long
                ? System.currentTimeMillis() - (Long) startTime
                : -1L;
        log.info("Completed request: {} {} -> {} ({}ms) traceId={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                elapsedMs,
                MDC.get("traceId"));
    }
}
