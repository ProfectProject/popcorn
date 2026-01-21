package com.popcorn.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 *
 * [Java 초보자를 위한 가이드]
 *
 * 캐시를 사용하는 이유:
 * 1. **성능 향상**: 동일한 데이터를 반복 조회할 때 DB/API 호출 없이 메모리에서 즉시 반환
 * 2. **네트워크 비용 절감**: Store 서비스 API 호출 횟수 감소
 * 3. **사용자 경험 개선**: 응답 시간 단축
 *
 * 캐시 전략:
 * - **Store 정보**: 자주 조회되고 변경이 적음 → 긴 TTL (30분)
 * - **팝업 정보**: 자주 조회되지만 변경 가능 → 중간 TTL (15분)
 * - **주문 목록**: 자주 조회되고 실시간성 중요 → 짧은 TTL (5분)
 *
 * Redis를 선택한 이유:
 * - 메모리 기반으로 매우 빠름
 * - 마이크로서비스 간 공유 캐시 구성 가능
 * - 다양한 데이터 타입 지원
 * - 클러스터링 지원으로 확장성 좋음
 */
@Configuration
@EnableCaching // Spring 캐시 기능 활성화
@Slf4j
public class CacheConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Redis 캐시 매니저 설정
     *
     * [Java 초보자 설명]
     * CacheManager는 캐시의 전체적인 동작을 관리하는 매니저입니다.
     * 여러 종류의 캐시를 다른 설정으로 운영할 수 있습니다.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer())) // 키는 문자열로 직렬화
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer())) // 값은 JSON으로 직렬화
                .disableCachingNullValues(); // null 값은 캐시하지 않음

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Store 서비스 캐시 (30분) - 매장 정보는 자주 변경되지 않음
        cacheConfigurations.put("store-info", defaultConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("order:store:"));

        // 팝업 정보 캐시 (15분) - 팝업 정보는 가끔 변경됨
        cacheConfigurations.put("popup-info", defaultConfig
                .entryTtl(Duration.ofMinutes(15))
                .prefixCacheNameWith("order:popup:"));

        // 주문 목록 캐시 (5분) - 실시간성이 중요한 주문 데이터
        cacheConfigurations.put("order-list", defaultConfig
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("order:list:"));

        // 사용자별 주문 캐시 (3분) - 사용자가 자주 조회하는 내 주문 목록
        cacheConfigurations.put("my-orders", defaultConfig
                .entryTtl(Duration.ofMinutes(3))
                .prefixCacheNameWith("order:user:"));

        log.info("🔧 Redis 캐시 매니저 설정 완료 - Redis: {}:{}, 캐시 종류: {}개",
                redisHost, redisPort, cacheConfigurations.size());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Redis Template 설정 (직접 Redis 조작이 필요한 경우)
     *
     * [Java 초보자 설명]
     * RedisTemplate는 Spring의 @Cacheable 어노테이션 외에
     * 직접 Redis에 데이터를 저장하거나 조회할 때 사용합니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 키와 값의 직렬화 방식 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();

        log.info("🔧 Redis Template 설정 완료");
        return template;
    }

    /**
     * 캐시 키 생성 전략
     *
     * [Java 초보자 설명]
     * 캐시 키 네이밍 컨벤션:
     * - 서비스명:기능:파라미터
     * - 예: "order:store:550e8400-e29b-41d4-a716-446655440000"
     * - 예: "order:popup:550e8400-e29b-41d4-a716-446655440001"
     *
     * 이렇게 하는 이유:
     * 1. 다른 마이크로서비스와 키 충돌 방지
     * 2. Redis 관리 도구에서 쉽게 식별 가능
     * 3. 캐시 무효화 시 패턴으로 삭제 가능
     */
    public static class CacheKeyGenerator {

        /**
         * Store 서비스 캐시 키 생성
         */
        public static String storeKey(String storeId) {
            return "order:store:" + storeId;
        }

        /**
         * 팝업 캐시 키 생성
         */
        public static String popupKey(String popupId) {
            return "order:popup:" + popupId;
        }

        /**
         * 사용자별 주문 목록 캐시 키 생성
         */
        public static String userOrdersKey(Long userId, String orderType, String status) {
            return String.format("order:user:%d:%s:%s", userId,
                    orderType != null ? orderType : "ALL",
                    status != null ? status : "ALL");
        }
    }
}