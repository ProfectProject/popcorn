package com.popcorn.demo.domain.auth.repository;

import org.springframework.data.repository.CrudRepository;

import com.popcorn.demo.domain.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken,String> {
    Optional<RefreshToken> findRefreshTokenByJwtRefreshToken(String refreshToken);
}

