package com.popcorn.demo.common.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class CacheConfig {

	private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

	// 📚 Common-Lib 기본 캐시 TTL 설정
	private static final Duration ORDER_DETAIL_TTL = Duration.ofMinutes(5);
	private static final Duration ORDER_LIST_TTL = Duration.ofMinutes(3);
	private static final Duration CUSTOMER_TIMELINE_TTL = Duration.ofMinutes(10);
	private static final Duration POPUP_LIST_TTL = Duration.ofMinutes(2);
	private static final Duration POPUP_DETAIL_TTL = Duration.ofMinutes(5);
	private static final Duration POPUP_SESSION_TTL = Duration.ofMinutes(3);
	private static final Duration POPUP_OPTION_TTL = Duration.ofMinutes(3);

	@Bean
	@Primary
	public CacheManager cacheManager(
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

		var cacheConfigs = Map.of(
			"orderDetails", defaultConfig.entryTtl(ORDER_DETAIL_TTL),
			"orderDetailsComplete", defaultConfig.entryTtl(ORDER_DETAIL_TTL),
			"storeOrders", defaultConfig.entryTtl(ORDER_LIST_TTL),
			"customerTimeline", defaultConfig.entryTtl(CUSTOMER_TIMELINE_TTL),
			"popupList", defaultConfig.entryTtl(POPUP_LIST_TTL),
			"popupDetail", defaultConfig.entryTtl(POPUP_DETAIL_TTL),
			"popupSessions", defaultConfig.entryTtl(POPUP_SESSION_TTL),
			"popupOptions", defaultConfig.entryTtl(POPUP_OPTION_TTL)
		);

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(defaultConfig)
			.withInitialCacheConfigurations(cacheConfigs)
			.build();
	}

	@Bean
	public CacheErrorHandler cacheErrorHandler() {
		return new CacheErrorHandler() {
			@Override
			public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
				log.warn("Cache get error. cache={}, key={}, message={}",
					cache != null ? cache.getName() : "unknown", key, exception.getMessage());
			}

			@Override
			public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
				log.warn("Cache put error. cache={}, key={}, message={}",
					cache != null ? cache.getName() : "unknown", key, exception.getMessage());
			}

			@Override
			public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
				log.warn("Cache evict error. cache={}, key={}, message={}",
					cache != null ? cache.getName() : "unknown", key, exception.getMessage());
			}

			@Override
			public void handleCacheClearError(RuntimeException exception, Cache cache) {
				log.warn("Cache clear error. cache={}, message={}",
					cache != null ? cache.getName() : "unknown", exception.getMessage());
			}
		};
	}
}
