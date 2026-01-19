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

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

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
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
				new GenericJackson2JsonRedisSerializer(objectMapper)))
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
}
