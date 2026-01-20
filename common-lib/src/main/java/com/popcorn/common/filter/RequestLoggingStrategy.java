package com.popcorn.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 로깅 전략 인터페이스
 *
 * 다양한 로깅 방식을 지원하기 위한 전략 패턴 구현
 * - 기본 로깅: 요청/응답 기본 정보
 * - 상세 로깅: 헤더, 파라미터 포함
 * - 성능 로깅: 처리 시간 중심
 * - 보안 로깅: 보안 관련 정보 포함
 */
public interface RequestLoggingStrategy {

    /**
     * 요청 처리 전 로깅 여부 결정
     *
     * @param request HTTP 요청
     * @return true면 로깅 제외, false면 로깅 수행
     */
    boolean shouldSkipLogging(HttpServletRequest request);

    /**
     * 요청 시작 시 로깅
     *
     * @param request HTTP 요청
     * @param startTime 요청 시작 시간
     */
    void logRequestStart(HttpServletRequest request, long startTime);

    /**
     * 요청 완료 시 로깅
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param startTime 요청 시작 시간
     * @param elapsedMs 처리 시간 (밀리초)
     */
    void logRequestComplete(
        HttpServletRequest request,
        HttpServletResponse response,
        long startTime,
        long elapsedMs
    );

    /**
     * 요청 처리 중 예외 발생 시 로깅
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param startTime 요청 시작 시간
     * @param elapsedMs 처리 시간 (밀리초)
     * @param exception 발생한 예외
     */
    default void logRequestError(
        HttpServletRequest request,
        HttpServletResponse response,
        long startTime,
        long elapsedMs,
        Exception exception
    ) {
        // 기본 구현: logRequestComplete와 동일하게 처리
        logRequestComplete(request, response, startTime, elapsedMs);
    }
}