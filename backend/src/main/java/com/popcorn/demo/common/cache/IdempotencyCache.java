package com.popcorn.demo.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class IdempotencyCache {

    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public boolean isDuplicate(String key) {
        return cache.getIfPresent(key) != null;
    }

    public void mark(String key) {
        cache.put(key, Boolean.TRUE);
    }
}
