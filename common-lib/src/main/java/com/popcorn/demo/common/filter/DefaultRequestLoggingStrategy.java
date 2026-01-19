package com.popcorn.demo.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 기본 요청 로깅 전략 구현체
 *
 * - API 요청/응답의 기본 정보를 로깅합니다 (메서드, URI, 상태코드, 처리시간)
 * - Swagger/Actuator 등 시스템 경로는 노이즈를 줄이기 위해 제외합니다
 * - 요청 바디는 로그에 남기지 않습니다 (민감정보/용량 문제 방지)
 */
@Component
public class DefaultRequestLoggingStrategy implements RequestLoggingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DefaultRequestLoggingStrategy.class);

    @Override
    public boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars")
                || "/swagger-ui.html".equals(path);
    }

    @Override
    public void logRequestStart(HttpServletRequest request, long startTime) {
        // 기본 구현에서는 요청 시작 시 별도 로깅하지 않음
        // 필요한 경우 하위 구현체에서 오버라이드 가능
    }

    @Override
    public void logRequestComplete(
        HttpServletRequest request,
        HttpServletResponse response,
        long startTime,
        long elapsedMs
    ) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null ? uri : uri + "?" + query;
        int status = response.getStatus();

        log.info("API {} {} -> {} ({} ms)", method, path, status, elapsedMs);
    }

    @Override
    public void logRequestError(
        HttpServletRequest request,
        HttpServletResponse response,
        long startTime,
        long elapsedMs,
        Exception exception
    ) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null ? uri : uri + "?" + query;
        int status = response.getStatus();

        log.error("API {} {} -> {} ({} ms) ERROR: {}",
                method, path, status, elapsedMs, exception.getMessage());
    }
}