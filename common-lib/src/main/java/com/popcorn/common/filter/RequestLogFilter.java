package com.popcorn.common.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 로깅 필터 인터페이스
 *
 * 다양한 로깅 필터 구현을 지원하기 위한 추상화
 * - 기본 로깅 필터: RequestLoggingFilterImpl
 * - 성능 중심 필터: PerformanceLoggingFilter
 * - 보안 중심 필터: SecurityLoggingFilter
 * - 커스텀 필터: 사용자 정의 구현
 */
public interface RequestLogFilter {

    /**
     * 요청이 필터링 대상인지 확인
     *
     * @param request HTTP 요청
     * @return true면 필터링 제외, false면 필터링 수행
     */
    boolean shouldSkipRequest(HttpServletRequest request);

    /**
     * 요청 로깅 처리를 수행
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException I/O 예외
     */
    void doFilter(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException;

    /**
     * 필터 초기화
     *
     * @param config 필터 설정
     */
    default void initialize(FilterConfig config) {
        // 기본 구현은 빈 메서드
    }

    /**
     * 필터 정리
     */
    default void cleanup() {
        // 기본 구현은 빈 메서드
    }

    /**
     * 필터 설정 인터페이스
     */
    interface FilterConfig {
        /**
         * 설정 값 조회
         */
        String getConfigValue(String key);

        /**
         * 설정 값 조회 (기본값 포함)
         */
        default String getConfigValue(String key, String defaultValue) {
            String value = getConfigValue(key);
            return value != null ? value : defaultValue;
        }
    }
}