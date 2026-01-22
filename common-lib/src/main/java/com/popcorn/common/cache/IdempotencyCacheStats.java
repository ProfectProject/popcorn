package com.popcorn.common.cache;

/**
 * 멱등성 캐시 통계 인터페이스
 *
 * 다양한 캐시 구현체의 통계를 제공하기 위한 인터페이스
 * - Caffeine 기반 구현체
 * - Redis 기반 구현체
 * - 데이터베이스 기반 구현체
 * - 커스텀 구현체
 */
public interface IdempotencyCacheStats {

    // Caffeine 캐시 기본 통계
    long getHitCount();              // 캐시 히트 횟수
    long getMissCount();             // 캐시 미스 횟수
    double getHitRate();             // 캐시 히트율 (0.0 ~ 1.0)
    long getTotalLoadTime();         // 총 로드 시간 (나노초)
    long getEvictionCount();         // 캐시 제거 횟수
    long getCacheSize();             // 현재 캐시 크기

    // 멱등성 서비스 전용 통계
    int getInProgressRequestCount();  // 현재 진행 중인 요청 수
    long getNewRequestCount();        // 새로운 요청 수 (전체)
    long getCacheHitCount();          // 캐시에서 응답한 요청 수
    long getConcurrentRequestCount(); // 동시 요청으로 거부된 수
    long getOperationErrorCount();    // 작업 실행 오류 수

    /**
     * 캐시 효율성 계산
     */
    default double getCacheEfficiency() {
        long totalRequests = getHitCount() + getMissCount();
        return totalRequests > 0 ? (double) getHitCount() / totalRequests : 0.0;
    }

    /**
     * 평균 로드 시간 (밀리초)
     */
    default double getAverageLoadTimeMs() {
        return getMissCount() > 0 ? getTotalLoadTime() / 1_000_000.0 / getMissCount() : 0.0;
    }

    /**
     * 동시성 문제 비율
     */
    default double getConcurrencyIssueRate() {
        long totalRequests = getNewRequestCount() + getCacheHitCount() + getConcurrentRequestCount();
        return totalRequests > 0 ? (double) getConcurrentRequestCount() / totalRequests : 0.0;
    }

    /**
     * 오류 발생율
     */
    default double getErrorRate() {
        long totalRequests = getNewRequestCount() + getCacheHitCount() + getConcurrentRequestCount();
        return totalRequests > 0 ? (double) getOperationErrorCount() / totalRequests : 0.0;
    }

    /**
     * 통계 정보를 읽기 쉬운 문자열로 변환
     */
    default String getFormattedStats() {
        return String.format("""
				💾 멱등성 캐시 통계
				━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
				🎯 캐시 성능
				   - 히트율: %.2f%% (%d hits / %d total)
				   - 캐시 크기: %d entries
				   - 평균 로드 시간: %.2f ms
				   - 제거된 항목: %d

				🔄 요청 처리
				   - 새로운 요청: %d
				   - 캐시된 응답: %d
				   - 진행 중: %d
				   - 동시 거부: %d (%.2f%%)

				❌ 오류 통계
				   - 실행 오류: %d (%.2f%%)
				━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
				""",
                getHitRate() * 100, getHitCount(), getHitCount() + getMissCount(),
                getCacheSize(),
                getAverageLoadTimeMs(),
                getEvictionCount(),
                getNewRequestCount(),
                getCacheHitCount(),
                getInProgressRequestCount(),
                getConcurrentRequestCount(), getConcurrencyIssueRate() * 100,
                getOperationErrorCount(), getErrorRate() * 100
        );
    }
}