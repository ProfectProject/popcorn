package com.popcorn.demo.common.cache;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component

public class IdempotencyCache {



	private final Cache<String, Boolean> cache = Caffeine.newBuilder()

			.maximumSize(10_000)

			.expireAfterWrite(Duration.ofMinutes(10))

			.build();



	/**

	 * 멱등성 키 중복 여부 확인

	 */

	public boolean isDuplicate(String key) {

		return cache.getIfPresent(key) != null;

	}



	/**

	 * 멱등성 키 기록

	 */

	public void mark(String key) {

		cache.put(key, Boolean.TRUE);

	}

}

