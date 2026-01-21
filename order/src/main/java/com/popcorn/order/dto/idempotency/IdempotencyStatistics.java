package com.popcorn.order.dto.idempotency;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멱등성 시스템 통계 DTO
 *
 * 멱등성 시스템의 성능과 효과를 측정하는 다양한 지표들을 담고 있습니다.
 * 관리자 대시보드, 모니터링 시스템, 성능 분석에 활용됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyStatistics {

    /** 통계 기간 */
    @Schema(description = "통계 기간", allowableValues = {"HOURLY", "DAILY", "WEEKLY", "MONTHLY"}, example = "DAILY")
    private String period;

    /** 통계 시작 시간 */
    @Schema(description = "통계 시작 시간", example = "2024-03-15T00:00:00")
    private LocalDateTime startTime;

    /** 통계 종료 시간 */
    @Schema(description = "통계 종료 시간", example = "2024-03-15T23:59:59")
    private LocalDateTime endTime;

    /** 총 생성된 키 수 */
    @Schema(description = "총 생성된 멱등성 키 수", example = "1250")
    private Long totalKeysGenerated;

    /** 사용된 키 수 */
    @Schema(description = "사용된 키 수", example = "980")
    private Long totalKeysUsed;

    /** 만료된 키 수 */
    @Schema(description = "만료된 키 수", example = "120")
    private Long totalKeysExpired;

    /** 실패한 키 수 */
    @Schema(description = "실패한 키 수", example = "15")
    private Long totalKeysFailed;

    /** 차단된 중복 요청 수 */
    @Schema(description = "차단된 중복 요청 수", example = "350")
    private Long totalRequestsBlocked;

    /** 키 사용률 (%) */
    @Schema(description = "키 사용률 (%)", example = "78.4")
    private Double keyUsageRate;

    /** 중복 요청 차단률 (%) */
    @Schema(description = "중복 요청 차단률 (%)", example = "26.3")
    private Double duplicateRequestBlockRate;

    /** 평균 키 사용 시간 (분) */
    @Schema(description = "평균 키 사용 시간 (분)", example = "5.2")
    private Double averageKeyUsageTimeMinutes;

    /** 최대 키 사용 시간 (분) */
    @Schema(description = "최대 키 사용 시간 (분)", example = "45.8")
    private Double maxKeyUsageTimeMinutes;

    /** 시간대별 키 생성 분포 */
    @Schema(description = "시간대별 키 생성 분포 (시간: 개수)")
    private Map<String, Long> keyGenerationByHour;

    /** 상태별 키 분포 */
    @Schema(description = "상태별 키 분포 (상태: 개수)")
    private Map<String, Long> keysByStatus;

    /** 사용자별 키 사용 Top 10 */
    @Schema(description = "사용자별 키 사용 Top 10 (사용자ID: 사용횟수)")
    private Map<String, Long> topKeyUsers;

    /** 일별 중복 요청 차단 추이 */
    @Schema(description = "일별 중복 요청 차단 추이 (날짜: 차단수)")
    private Map<String, Long> dailyBlockedRequests;

    /** 평균 처리 시간 (밀리초) */
    @Schema(description = "평균 멱등성 검사 처리 시간 (ms)", example = "12.5")
    private Double averageProcessingTimeMs;

    /** 시스템 절약 효과 (차단된 요청 수) */
    @Schema(description = "시스템 부하 절약 효과 (차단된 중복 처리)", example = "350")
    private Long systemLoadSaved;

    // ========================= 계산된 메트릭 =========================

    /**
     * 전체 효율성 점수 계산 (0-100점)
     * 키 사용률과 중복 차단률을 종합한 점수
     * @return 효율성 점수
     */
    public Double getEfficiencyScore() {
        if (keyUsageRate == null || duplicateRequestBlockRate == null) {
            return null;
        }
        // 키 사용률 70% + 중복 차단률 30% 가중치
        return (keyUsageRate * 0.7) + (duplicateRequestBlockRate * 0.3);
    }

    /**
     * 시스템 건강도 평가
     * @return 건강도 등급 (EXCELLENT, GOOD, FAIR, POOR)
     */
    public String getSystemHealth() {
        Double efficiency = getEfficiencyScore();
        if (efficiency == null) return "UNKNOWN";

        if (efficiency >= 80) return "EXCELLENT";
        else if (efficiency >= 60) return "GOOD";
        else if (efficiency >= 40) return "FAIR";
        else return "POOR";
    }

    /**
     * 키 낭비율 계산 (%)
     * 생성되었지만 사용되지 않고 만료된 키의 비율
     * @return 낭비율 (%)
     */
    public Double getKeyWasteRate() {
        if (totalKeysGenerated == null || totalKeysExpired == null) {
            return null;
        }
        if (totalKeysGenerated == 0) return 0.0;

        return (totalKeysExpired.doubleValue() / totalKeysGenerated.doubleValue()) * 100;
    }

    /**
     * 중복 요청 강도 계산
     * 사용된 키 대비 차단된 중복 요청의 비율
     * @return 중복 요청 강도
     */
    public Double getDuplicateRequestIntensity() {
        if (totalKeysUsed == null || totalRequestsBlocked == null) {
            return null;
        }
        if (totalKeysUsed == 0) return 0.0;

        return totalRequestsBlocked.doubleValue() / totalKeysUsed.doubleValue();
    }

    /**
     * 가장 활발한 시간대 조회
     * @return 가장 많은 키가 생성된 시간대
     */
    public String getPeakHour() {
        if (keyGenerationByHour == null || keyGenerationByHour.isEmpty()) {
            return null;
        }

        return keyGenerationByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 성능 등급 평가
     * 평균 처리 시간을 기준으로 성능 등급 산정
     * @return 성능 등급
     */
    public String getPerformanceGrade() {
        if (averageProcessingTimeMs == null) return "UNKNOWN";

        if (averageProcessingTimeMs < 10) return "A+";
        else if (averageProcessingTimeMs < 20) return "A";
        else if (averageProcessingTimeMs < 50) return "B";
        else if (averageProcessingTimeMs < 100) return "C";
        else return "D";
    }

    /**
     * 권장사항 생성
     * 통계를 바탕으로 시스템 개선 권장사항 제시
     * @return 권장사항 목록
     */
    public java.util.List<String> getRecommendations() {
        java.util.List<String> recommendations = new java.util.ArrayList<>();

        if (getKeyWasteRate() != null && getKeyWasteRate() > 30) {
            recommendations.add("키 만료 시간을 단축하여 리소스 낭비를 줄이세요");
        }

        if (keyUsageRate != null && keyUsageRate < 50) {
            recommendations.add("클라이언트의 키 생성 로직을 개선하세요");
        }

        if (averageProcessingTimeMs != null && averageProcessingTimeMs > 50) {
            recommendations.add("멱등성 검사 성능을 최적화하세요");
        }

        if (getDuplicateRequestIntensity() != null && getDuplicateRequestIntensity() > 2.0) {
            recommendations.add("클라이언트 재시도 로직을 개선하거나 네트워크 안정성을 확인하세요");
        }

        return recommendations;
    }

    /**
     * 요약 정보를 한 줄로 표현
     * @return 통계 요약 문자열
     */
    public String getSummary() {
        return String.format("키생성:%d, 사용:%d(%.1f%%), 중복차단:%d, 효율성:%.1f점",
                totalKeysGenerated != null ? totalKeysGenerated : 0,
                totalKeysUsed != null ? totalKeysUsed : 0,
                keyUsageRate != null ? keyUsageRate : 0.0,
                totalRequestsBlocked != null ? totalRequestsBlocked : 0,
                getEfficiencyScore() != null ? getEfficiencyScore() : 0.0);
    }

}