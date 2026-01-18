package com.popcorn.demo.domain.auth.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 60*60*24*14)
public class RefreshToken {

    @Id
    @Indexed
    private String jwtRefreshToken;

    private String authKey;

    @TimeToLive
    private Long ttl; // 현재 14일 , 만료시기와 같게 수정

    @Builder
    public RefreshToken(String jwtRefreshToken,String authKey,Long ttl) {
        this.jwtRefreshToken = jwtRefreshToken;
        this.authKey = authKey;
        this.ttl = ttl;
    }
}
