package com.popcorn.order.dto.audit;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 로그 엔트리 DTO
 *
 * 각각의 API 호출에 대한 상세 정보를 담는 데이터 클래스입니다.
 * 감사 로그, 성능 분석, 보안 모니터링에 활용됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLogEntry {

    /** 요청 추적용 고유 ID */
    private String requestId;

    /** HTTP 메서드 (GET, POST, PUT, DELETE 등) */
    private String method;

    /** 요청 URL */
    private String url;

    /** 쿼리 스트링 */
    private String queryString;

    /** 클라이언트 IP 주소 */
    private String clientIp;

    /** 사용자 에이전트 (브라우저/앱 정보) */
    private String userAgent;

    /** 세션 ID */
    private String sessionId;

    /** 요청한 사용자 ID */
    private Long userId;

    /** 요청 시작 시간 */
    private LocalDateTime requestTime;

    /** 요청 완료 시간 */
    private LocalDateTime responseTime;

    /** 처리 소요 시간 (밀리초) */
    private Long processingTimeMs;

    /** HTTP 응답 상태 코드 */
    private Integer responseStatus;

    /** 컨트롤러 클래스명 */
    private String controllerClass;

    /** 호출된 메서드명 */
    private String methodName;

    /** 오류 정보 (예외 발생시) */
    private String errorMessage;

    /** 오류 스택 트레이스 (예외 발생시) */
    private String stackTrace;

    /** 요청 본문 크기 (바이트) */
    private Long requestSize;

    /** 응답 본문 크기 (바이트) */
    private Long responseSize;

    /** Referer 헤더 */
    private String referer;

    /** 추가 메타데이터 (JSON 문자열) */
    private String metadata;

    // ========================= 편의 메서드 =========================

    /**
     * 요청이 성공했는지 확인
     * @return 2xx 상태코드면 true
     */
    public boolean isSuccess() {
        return responseStatus != null && responseStatus >= 200 && responseStatus < 300;
    }

    /**
     * 클라이언트 오류인지 확인
     * @return 4xx 상태코드면 true
     */
    public boolean isClientError() {
        return responseStatus != null && responseStatus >= 400 && responseStatus < 500;
    }

    /**
     * 서버 오류인지 확인
     * @return 5xx 상태코드면 true
     */
    public boolean isServerError() {
        return responseStatus != null && responseStatus >= 500;
    }

    /**
     * 느린 요청인지 확인 (1초 초과)
     * @return 1초 초과면 true
     */
    public boolean isSlowRequest() {
        return processingTimeMs != null && processingTimeMs > 1000;
    }

    /**
     * 매우 느린 요청인지 확인 (3초 초과)
     * @return 3초 초과면 true
     */
    public boolean isVerySlowRequest() {
        return processingTimeMs != null && processingTimeMs > 3000;
    }

    /**
     * 인증된 요청인지 확인
     * @return 사용자 ID가 있으면 true
     */
    public boolean isAuthenticatedRequest() {
        return userId != null;
    }

    /**
     * 전체 URL (쿼리 스트링 포함) 반환
     * @return 완전한 URL
     */
    public String getFullUrl() {
        if (queryString != null && !queryString.isEmpty()) {
            return url + "?" + queryString;
        }
        return url;
    }

    /**
     * 응답 상태 설명 반환
     * @return 상태 코드에 대한 설명
     */
    public String getStatusDescription() {
        if (responseStatus == null) return "Unknown";

        return switch (responseStatus) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "HTTP " + responseStatus;
        };
    }

    /**
     * 성능 등급 반환
     * @return 처리 시간에 따른 성능 등급
     */
    public String getPerformanceGrade() {
        if (processingTimeMs == null) return "N/A";

        if (processingTimeMs < 100) return "A+";
        else if (processingTimeMs < 300) return "A";
        else if (processingTimeMs < 500) return "B";
        else if (processingTimeMs < 1000) return "C";
        else if (processingTimeMs < 3000) return "D";
        else return "F";
    }

    /**
     * 로그 요약 정보 반환
     * @return 한 줄로 요약된 로그 정보
     */
    public String getSummary() {
        return String.format("%s %s -> %d (%dms) [%s]",
                method, url, responseStatus != null ? responseStatus : 0,
                processingTimeMs != null ? processingTimeMs : 0,
                userId != null ? "User:" + userId : "Anonymous");
    }

}