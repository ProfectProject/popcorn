package com.popcorn.demo.common.cache;

import lombok.Builder;
import lombok.Getter;

/**
 * Caffeine 기반 멱등성 캐시 통계 구현체
 *
 * IdempotencyCacheStats 인터페이스의 Caffeine 캐시 전용 구현
 */
@Getter
@Builder
public class CaffeineIdempotencyCacheStats implements IdempotencyCacheStats {

    // Caffeine 캐시 기본 통계
    private final long hitCount;              // 캐시 히트 횟수
    private final long missCount;             // 캐시 미스 횟수
    private final double hitRate;             // 캐시 히트율 (0.0 ~ 1.0)
    private final long totalLoadTime;        // 총 로드 시간 (나노초)
    private final long evictionCount;         // 캐시 제거 횟수
    private final long cacheSize;             // 현재 캐시 크기

    // 멱등성 서비스 전용 통계
    private final int inProgressRequestCount;  // 현재 진행 중인 요청 수
    private final long newRequestCount;        // 새로운 요청 수 (전체)
    private final long cacheHitCount;          // 캐시에서 응답한 요청 수
    private final long concurrentRequestCount; // 동시 요청으로 거부된 수
    private final long operationErrorCount;    // 작업 실행 오류 수

    @Override
    public String toString() {
        return getFormattedStats();
    }
}