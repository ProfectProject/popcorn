package com.popcorn.order.dto.idempotency;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 멱등성 통계 응답 DTO
 *
 * 멱등성 키 사용 통계와 시스템 성능 지표를 담습니다.
 * 모니터링, 운영 관리, 성능 분석에 활용됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyStatisticsResponse {

    /** 통계 조회 시작 시간 */
    @Schema(description = "조회 시작 시간")
    private LocalDateTime startTime;

    /** 통계 조회 종료 시간 */
    @Schema(description = "조회 종료 시간")
    private LocalDateTime endTime;

    /** 통계 집계 기간 (HOURLY, DAILY, WEEKLY, MONTHLY) */
    @Schema(description = "집계 기간", example = "DAILY")
    private String period;

    /** 전체 생성된 키 수 */
    @Schema(description = "생성된 키 수", example = "1250")
    private Long totalKeysGenerated;

    /** 사용된 키 수 */
    @Schema(description = "사용된 키 수", example = "980")
    private Long totalKeysUsed;

    /** 만료된 키 수 */
    @Schema(description = "만료된 키 수", example = "150")
    private Long totalKeysExpired;

    /** 실패한 키 수 */
    @Schema(description = "실패한 키 수", example = "120")
    private Long totalKeysFailed;

    /** 중복 요청 총 횟수 */
    @Schema(description = "중복 요청 횟수", example = "45")
    private Long totalDuplicateRequests;

    /** 차단된 중복 요청 수 */
    @Schema(description = "차단된 중복 요청 수", example = "42")
    private Long totalBlockedRequests;

    /** 키 사용률 (사용된키/생성된키) */
    @Schema(description = "키 사용률 (%)", example = "78.4")
    private Double keyUsageRate;

    /** 중복 요청 비율 (중복요청/전체요청) */
    @Schema(description = "중복 요청 비율 (%)", example = "4.6")
    private Double duplicateRequestRate;

    /** 평균 키 생존 시간 (분 단위) */
    @Schema(description = "평균 키 생존 시간 (분)", example = "25.6")
    private Double averageKeyLifetimeMinutes;

    /** 평균 키 사용까지 대기 시간 (분 단위) */
    @Schema(description = "평균 사용 대기 시간 (분)", example = "3.2")
    private Double averageUsageDelayMinutes;

    /** 시간대별 키 생성 통계 */
    @Schema(description = "시간대별 생성 통계")
    private List<HourlyStats> hourlyGenerationStats;

    /** 시간대별 키 사용 통계 */
    @Schema(description = "시간대별 사용 통계")
    private List<HourlyStats> hourlyUsageStats;

    /** 사용자별 키 사용 Top 10 */
    @Schema(description = "사용자별 통계 Top 10")
    private List<UserStats> topUserStats;

    /** 요청 타입별 분포 */
    @Schema(description = "요청 타입별 분포")
    private Map<String, Long> requestTypeDistribution;

    /** 클라이언트 IP별 분포 Top 10 */
    @Schema(description = "클라이언트 IP별 분포 Top 10")
    private Map<String, Long> clientIpDistribution;

    /** 오류 타입별 통계 */
    @Schema(description = "오류 타입별 통계")
    private Map<String, Long> errorTypeDistribution;

    /** 월별 트렌드 데이터 (최근 12개월) */
    @Schema(description = "월별 트렌드")
    private List<MonthlyTrend> monthlyTrends;

    /** 성능 지표 */
    @Schema(description = "성능 지표")
    private PerformanceMetrics performanceMetrics;

    /** 통계 생성 시간 */
    @Schema(description = "통계 생성 시간")
    private LocalDateTime generatedAt;

    // ========================= 내부 클래스 =========================

    /**
     * 시간대별 통계
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyStats {
        @Schema(description = "시간", example = "14")
        private Integer hour;

        @Schema(description = "카운트", example = "125")
        private Long count;

        @Schema(description = "날짜", example = "2024-03-15")
        private String date;
    }

    /**
     * 사용자별 통계
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @Schema(description = "키 생성 수", example = "25")
        private Long keysGenerated;

        @Schema(description = "키 사용 수", example = "23")
        private Long keysUsed;

        @Schema(description = "중복 요청 수", example = "3")
        private Long duplicateRequests;

        @Schema(description = "사용률 (%)", example = "92.0")
        private Double usageRate;
    }

    /**
     * 월별 트렌드
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyTrend {
        @Schema(description = "월", example = "2024-03")
        private String month;

        @Schema(description = "생성된 키 수", example = "5500")
        private Long keysGenerated;

        @Schema(description = "사용된 키 수", example = "4800")
        private Long keysUsed;

        @Schema(description = "중복 요청 수", example = "250")
        private Long duplicateRequests;

        @Schema(description = "성장률 (%)", example = "15.5")
        private Double growthRate;
    }

    /**
     * 성능 지표
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerformanceMetrics {
        @Schema(description = "평균 검증 시간 (ms)", example = "2.5")
        private Double averageValidationTimeMs;

        @Schema(description = "최대 검증 시간 (ms)", example = "15.2")
        private Double maxValidationTimeMs;

        @Schema(description = "95퍼센타일 검증 시간 (ms)", example = "8.1")
        private Double p95ValidationTimeMs;

        @Schema(description = "초당 처리 요청 수", example = "150.5")
        private Double requestsPerSecond;

        @Schema(description = "메모리 사용량 (MB)", example = "45.2")
        private Double memoryUsageMB;

        @Schema(description = "캐시 히트율 (%)", example = "94.5")
        private Double cacheHitRate;
    }

    // ========================= 편의 메서드 =========================

    /**
     * 전체 키 수 계산
     * @return 전체 키 수
     */
    public long getTotalKeys() {
        return totalKeysGenerated != null ? totalKeysGenerated : 0;
    }

    /**
     * 활성 키 수 계산 (생성됨 - 사용됨 - 만료됨 - 실패)
     * @return 활성 키 수
     */
    public long getActiveKeys() {
        return getTotalKeys() -
               (totalKeysUsed != null ? totalKeysUsed : 0) -
               (totalKeysExpired != null ? totalKeysExpired : 0) -
               (totalKeysFailed != null ? totalKeysFailed : 0);
    }

    /**
     * 키 효율성 점수 계산 (사용률 * (100 - 중복요청률))
     * @return 효율성 점수 (0-100)
     */
    public double getEfficiencyScore() {
        if (keyUsageRate == null) return 0.0;
        double duplicateRate = duplicateRequestRate != null ? duplicateRequestRate : 0.0;
        return keyUsageRate * (100.0 - duplicateRate) / 100.0;
    }

    /**
     * 피크 시간대 조회 (가장 많이 사용된 시간)
     * @return 피크 시간 (0-23)
     */
    public Integer getPeakHour() {
        if (hourlyUsageStats == null || hourlyUsageStats.isEmpty()) return null;

        return hourlyUsageStats.stream()
                .max((a, b) -> Long.compare(a.getCount(), b.getCount()))
                .map(HourlyStats::getHour)
                .orElse(null);
    }

    /**
     * 시스템 부하 레벨 계산
     * @return 부하 레벨 (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public String getSystemLoadLevel() {
        if (performanceMetrics == null) return "UNKNOWN";

        Double avgTime = performanceMetrics.getAverageValidationTimeMs();
        if (avgTime == null) return "UNKNOWN";

        if (avgTime < 5.0) return "LOW";
        if (avgTime < 10.0) return "MEDIUM";
        if (avgTime < 20.0) return "HIGH";
        return "CRITICAL";
    }

    /**
     * 통계 요약 정보
     * @return 통계 요약 문자열
     */
    public String getSummary() {
        return String.format("기간: %s | 생성: %,d | 사용: %,d (%.1f%%) | 중복: %,d (%.1f%%)",
                period,
                totalKeysGenerated != null ? totalKeysGenerated : 0,
                totalKeysUsed != null ? totalKeysUsed : 0,
                keyUsageRate != null ? keyUsageRate : 0.0,
                totalDuplicateRequests != null ? totalDuplicateRequests : 0,
                duplicateRequestRate != null ? duplicateRequestRate : 0.0);
    }

}