package com.popcorn.demo.common.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@Profile("!test")
public class RedisCacheConfig {

    private static final Duration ORDER_DETAIL_TTL = Duration.ofMinutes(5);
    private static final Duration ORDER_LIST_TTL = Duration.ofMinutes(3);
    private static final Duration CUSTOMER_TIMELINE_TTL = Duration.ofMinutes(10);

    @Bean(name = "redisCacheManager")
    public CacheManager redisCacheManager(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        ObjectMapper redisObjectMapper = objectMapper.copy()
            .activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Object.class)
                    .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
            );

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(redisObjectMapper)))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("orderDetails", defaultConfig.entryTtl(ORDER_DETAIL_TTL));
        cacheConfigs.put("orderDetailsComplete", defaultConfig.entryTtl(ORDER_DETAIL_TTL));
        cacheConfigs.put("storeOrders", defaultConfig.entryTtl(ORDER_LIST_TTL));
        cacheConfigs.put("customerTimeline", defaultConfig.entryTtl(CUSTOMER_TIMELINE_TTL));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
