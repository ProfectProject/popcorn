package com.popcorn.demo.common.config;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

import com.github.benmanes.caffeine.cache.Caffeine;

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
	public CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(List.of(
				buildCache("orderDetails", ORDER_DETAIL_TTL),
				buildCache("orderDetailsComplete", ORDER_DETAIL_TTL),
				buildCache("storeOrders", ORDER_LIST_TTL),
				buildCache("customerTimeline", CUSTOMER_TIMELINE_TTL),
				buildCache("popupList", POPUP_LIST_TTL),
				buildCache("popupDetail", POPUP_DETAIL_TTL),
				buildCache("popupSessions", POPUP_SESSION_TTL),
				buildCache("popupOptions", POPUP_OPTION_TTL)
		));
		return cacheManager;
	}

	private CaffeineCache buildCache(String name, Duration ttl) {
		return new CaffeineCache(name, Caffeine.newBuilder()
				.expireAfterWrite(ttl)
				.recordStats()
				.build());
	}
}