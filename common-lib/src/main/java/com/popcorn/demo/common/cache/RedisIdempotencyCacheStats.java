package com.popcorn.demo.common.cache;

import lombok.Builder;
import lombok.Getter;

/**
 * Redis 기반 멱등성 캐시 통계 구현체
 */
@Getter
@Builder
public class RedisIdempotencyCacheStats implements IdempotencyCacheStats {

    // Redis 캐시 기본 통계
    private final long hitCount;
    private final long missCount;
    private final double hitRate;
    private final long totalLoadTime;
    private final long evictionCount;
    private final long cacheSize;

    // 멱등성 서비스 전용 통계
    private final int inProgressRequestCount;
    private final long newRequestCount;
    private final long cacheHitCount;
    private final long concurrentRequestCount;
    private final long operationErrorCount;

    @Override
    public String toString() {
        return getFormattedStats();
    }
}
