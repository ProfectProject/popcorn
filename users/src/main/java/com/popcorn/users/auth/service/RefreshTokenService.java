package com.popcorn.users.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, long ttlMillis) {
        String key = buildKey(userId);
        stringRedisTemplate.opsForValue().set(key, refreshToken, Duration.ofMillis(ttlMillis));
        log.info("Saved refresh token. key={}, ttl={}s", key, stringRedisTemplate.getExpire(key));
    }

    public boolean isRefreshTokenValid(Long userId, String refreshToken) {
        String key = buildKey(userId);
        String stored = stringRedisTemplate.opsForValue().get(key);
        return refreshToken != null && refreshToken.equals(stored);
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
