package com.popcorn.common.filter;

import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 상세 요청 로깅 전략 구현체
 *
 * - 요청/응답의 상세 정보를 포함하여 로깅합니다 (헤더, 파라미터 등)
 * - 디버깅이나 개발 환경에서 사용하기 적합합니다
 * - 운영 환경에서는 로그 양이 많아질 수 있으므로 주의가 필요합니다
 */
public class DetailedRequestLoggingStrategy implements RequestLoggingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DetailedRequestLoggingStrategy.class);

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
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null ? uri : uri + "?" + query;
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();

        log.info("API REQUEST START: {} {} [Client: {}, UA: {}]",
                method, path, remoteAddr, userAgent);

        // 요청 헤더 로깅 (선택적)
        if (log.isDebugEnabled()) {
            StringBuilder headers = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headers.append(headerName).append(": ").append(headerValue).append("; ");
            }
            log.debug("Request headers: {}", headers);
        }
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
        String contentType = response.getContentType();

        log.info("API REQUEST COMPLETE: {} {} -> {} ({} ms) [Content-Type: {}]",
                method, path, status, elapsedMs, contentType);

        // 성능 경고
        if (elapsedMs > 5000) {
            log.warn("SLOW API DETECTED: {} {} took {} ms", method, path, elapsedMs);
        }
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

        log.error("API REQUEST ERROR: {} {} -> {} ({} ms) ERROR: {} [{}]",
                method, path, status, elapsedMs,
                exception.getClass().getSimpleName(), exception.getMessage());

        // 스택 트레이스는 DEBUG 레벨에서만
        if (log.isDebugEnabled()) {
            log.debug("Full stack trace:", exception);
        }
    }
}