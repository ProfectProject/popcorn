package com.popcorn.demo.domain.auth.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.auth.entity.RefreshToken;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import com.popcorn.demo.domain.auth.repository.RefreshTokenRepository;
import org.springframework.data.redis.core.RedisTemplate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    @Transactional
    public void saveRefreshToken(String refreshToken,String authKey ,Long ttl) {
        /*RefreshToken token = RefreshToken.builder()
                .jwtRefreshToken(refreshToken)
                .authKey(authKey)
                .ttl(ttl)
                .build();*/
        String key = "refresh:" + authKey;
        redisTemplate.opsForValue().set(key, refreshToken, ttl, TimeUnit.MILLISECONDS);
        
        //refreshTokenRepository.save(token);
    }

    // 토큰 조회
    public String getRefreshToken(String authKey) {
        return redisTemplate.opsForValue().get("refresh:" + authKey);
    }

    // accesstoken 재발급
    public String updateAccessToken(String authKey){
        String key = "refresh:" + authKey;
        Long expiredMs=1 * 60 * 1000L;
        String refreshToken = redisTemplate.opsForValue().get(key);
        // 토큰이 없는 경우 : authkey 가진 토큰 존재하는지 확인
        if (refreshToken == null) {
            throw new RuntimeException("Refresh Token does not exist");
        }
        // 만료된 경우 : 만료 확인 redisTemplate.opsForValue().ttl("refresh:" + authKey);
        Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        if (ttlMillis == null || ttlMillis <= 0) {
            throw new RuntimeException("Refresh Token expired");
        }

        // 토큰에서 정보 추출
        Long userId = jwtUtil.getUserId(refreshToken);
        String email = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String accesstoken = jwtUtil.createJwt(userId, email, role,expiredMs);

        // 새로운 accesstoken 생성 -> 반환
        return accesstoken;

    }

    // 토큰 삭제

}
