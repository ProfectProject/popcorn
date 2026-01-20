package com.popcorn.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 성능 중심 요청 로깅 전략 구현체
 *
 * - 성능 메트릭에 중점을 둔 로깅을 수행합니다
 * - 느린 API 감지 및 경고 기능을 제공합니다
 * - 응답 시간 통계 및 임계값 기반 알림을 지원합니다
 * - 운영 환경에서 성능 모니터링용으로 적합합니다
 */
public class PerformanceRequestLoggingStrategy implements RequestLoggingStrategy {

    private static final Logger log = LoggerFactory.getLogger(PerformanceRequestLoggingStrategy.class);

    // 성능 임계값 (밀리초)
    private static final long SLOW_REQUEST_THRESHOLD = 1000L;   // 1초
    private static final long VERY_SLOW_REQUEST_THRESHOLD = 5000L;  // 5초

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
        // 성능 로깅에서는 시작 로그를 간단하게 처리
        if (log.isDebugEnabled()) {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            log.debug("PERF START: {} {} [{}]", method, uri, startTime);
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

        // 성능 수준에 따른 로깅
        if (elapsedMs >= VERY_SLOW_REQUEST_THRESHOLD) {
            log.error("VERY SLOW API: {} {} -> {} ({} ms) [CRITICAL PERFORMANCE ISSUE]",
                    method, path, status, elapsedMs);
        } else if (elapsedMs >= SLOW_REQUEST_THRESHOLD) {
            log.warn("SLOW API: {} {} -> {} ({} ms) [PERFORMANCE WARNING]",
                    method, path, status, elapsedMs);
        } else {
            log.info("API: {} {} -> {} ({} ms)",
                    method, path, status, elapsedMs);
        }

        // 성능 메트릭 추가 정보
        logPerformanceMetrics(method, path, status, elapsedMs);
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

        // 에러와 성능을 함께 고려한 로깅
        if (elapsedMs >= SLOW_REQUEST_THRESHOLD) {
            log.error("SLOW ERROR API: {} {} -> {} ({} ms) ERROR: {} [PERFORMANCE + ERROR ISSUE]",
                    method, path, status, elapsedMs, exception.getMessage());
        } else {
            log.error("ERROR API: {} {} -> {} ({} ms) ERROR: {}",
                    method, path, status, elapsedMs, exception.getMessage());
        }
    }

    /**
     * 성능 메트릭 로깅
     */
    private void logPerformanceMetrics(String method, String path, int status, long elapsedMs) {
        // 향후 메트릭 수집 시스템과 연동 가능
        // 예: Micrometer, Prometheus, CloudWatch 등

        if (log.isDebugEnabled()) {
            log.debug("PERF METRICS: method={}, path={}, status={}, elapsed={}, " +
                    "slow={}, very_slow={}",
                    method, path, status, elapsedMs,
                    elapsedMs >= SLOW_REQUEST_THRESHOLD,
                    elapsedMs >= VERY_SLOW_REQUEST_THRESHOLD);
        }
    }
}