package com.popcorn.order.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis 캐시 설정
 *
 * 주문 서비스에서 자주 조회되는 데이터를 캐시하여 성능을 향상시킵니다.
 * 각 캐시 영역별로 다른 TTL(Time To Live)을 설정하여 최적화합니다.
 *
 * 캐시 전략:
 * - 주문 상세: 5분 (자주 변경되지 않음)
 * - 주문 목록: 2분 (페이징 결과)
 * - 사용자 주문 통계: 10분 (계산 비용이 높음)
 * - 이벤트 메트릭: 30분 (실시간성이 크게 중요하지 않음)
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Redis 캐시 매니저 설정
     *
     * 각 캐시 영역별로 개별적인 TTL과 설정을 적용합니다.
     * JSON 직렬화를 통해 객체를 Redis에 저장하고 조회합니다.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // 기본 TTL 5분
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(createJsonRedisSerializer()))
                .disableCachingNullValues();  // null 값은 캐시하지 않음

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 주문 상세 정보 캐시 - 5분
        cacheConfigurations.put("orderDetail", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // 주문 목록 캐시 - 2분 (변경이 빈번함)
        cacheConfigurations.put("orderList", defaultConfig
                .entryTtl(Duration.ofMinutes(2)));

        // 사용자별 주문 요약 - 10분 (계산 비용 높음)
        cacheConfigurations.put("userOrderSummary", defaultConfig
                .entryTtl(Duration.ofMinutes(10)));

        // 주문 통계 데이터 - 30분 (실시간성 낮음)
        cacheConfigurations.put("orderStatistics", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));

        // 이벤트 메트릭 - 1시간 (집계 데이터)
        cacheConfigurations.put("eventMetrics", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        // 멱등성 키 정보 - 24시간 (키 수명과 동일)
        cacheConfigurations.put("idempotencyKeys", defaultConfig
                .entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * RedisTemplate 설정
     *
     * 캐시 매니저가 아닌 직접적인 Redis 조작이 필요할 때 사용합니다.
     * 성능 모니터링, 수동 캐시 조작 등에 활용됩니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 키와 해시키는 문자열로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 값과 해시값은 JSON으로 직렬화
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 트랜잭션 지원 활성화
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * JSON 직렬화기 생성
     *
     * Java 객체를 JSON 문자열로 변환하여 Redis에 저장합니다.
     * LocalDateTime 등의 Java 8 시간 API를 지원하도록 설정합니다.
     */
    private GenericJackson2JsonRedisSerializer createJsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Java 8 시간 API 지원 (LocalDateTime 등)
        objectMapper.registerModule(new JavaTimeModule());

        // 타입 정보 포함하여 역직렬화 시 올바른 타입으로 변환
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // 알 수 없는 프로퍼티 무시 (하위 호환성)
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}