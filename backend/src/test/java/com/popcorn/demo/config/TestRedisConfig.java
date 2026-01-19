package com.popcorn.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.popcorn.demo.common.cache.IdempotencyCacheStats;
import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.common.cache.IdempotentOperation;
import com.popcorn.demo.common.cache.RedisIdempotencyCacheStats;

import lombok.extern.slf4j.Slf4j;

/**
 * 테스트용 설정
 *
 * 실제 Redis 대신 Mock 구현체를 제공하여
 * 테스트 환경에서 Redis 의존성을 제거합니다.
 */
@Configuration
@ConditionalOnClass(IdempotencyService.class)
@ConditionalOnMissingBean(name = "idempotencyService")
@Profile("test")
@Slf4j
public class TestRedisConfig {

    @Bean(name = "redisCacheManager")
    @Profile("test")
    public CacheManager testRedisCacheManager() {
        return new ConcurrentMapCacheManager(
            "orderDetails",
            "orderDetailsComplete",
            "storeOrders",
            "customerTimeline"
        );
    }

    @Bean
    @Primary
    public IdempotencyService testIdempotencyService() {
        return new MockIdempotencyService();
    }

    /**
     * 테스트용 Mock 멱등성 서비스
     * Redis 없이도 테스트가 실행될 수 있도록 합니다.
     */
    public static class MockIdempotencyService implements IdempotencyService {

        @Override
        public <T> IdempotencyResult<T> processRequest(String idempotencyKey, IdempotentOperation<T> operation, Class<T> responseType) {
            try {
                // 테스트에서는 실제 멱등성 로직 없이 바로 실행
                log.debug("🎯 Mock Idempotency Service 실행 - 키: {}", idempotencyKey);
                T result = operation.execute();
                return IdempotencyResult.newExecution(result);
            } catch (Exception e) {
                throw new IdempotencyException("Mock idempotency service execution failed", e);
            }
        }

        @Override
        public IdempotencyCacheStats getCacheStats() {
            return RedisIdempotencyCacheStats.builder()
                .hitCount(0L)
                .missCount(0L)
                .hitRate(0.0)
                .totalLoadTime(0L)
                .evictionCount(0L)
                .cacheSize(0L)
                .inProgressRequestCount(0)
                .newRequestCount(0L)
                .cacheHitCount(0L)
                .concurrentRequestCount(0L)
                .operationErrorCount(0L)
                .build();
        }

        @Override
        public void clearCache() {
            log.debug("🧹 Mock Cache 전체 초기화 (테스트용 - 실제 작업 없음)");
        }

        @Override
        public void invalidateKey(String idempotencyKey) {
            log.debug("🗑️ Mock Cache 키 무효화: {} (테스트용 - 실제 작업 없음)", idempotencyKey);
        }
    }
}
