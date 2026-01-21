package com.popcorn.store.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

	@Test
	void readsClaimsAndChecksExpiration() {
		byte[] keyBytes = new byte[32];
		for (int i = 0; i < keyBytes.length; i++) {
			keyBytes[i] = (byte)(i + 1);
		}
		String secret = Base64.getEncoder().encodeToString(keyBytes);
		JwtUtil jwtUtil = new JwtUtil(secret);

		SecretKey key = Keys.hmacShaKeyFor(keyBytes);
		String token = Jwts.builder()
				.claim("id", 123L)
				.claim("role", "OWNER")
				.setExpiration(new Date(System.currentTimeMillis() + 60_000))
				.signWith(key)
				.compact();

		assertThat(jwtUtil.getUserId(token)).isEqualTo(123L);
		assertThat(jwtUtil.getRole(token)).isEqualTo("OWNER");
		assertThat(jwtUtil.isExpired(token)).isFalse();
	}

	@Test
	void detectsExpiredToken() {
		byte[] keyBytes = new byte[32];
		for (int i = 0; i < keyBytes.length; i++) {
			keyBytes[i] = (byte)(i + 2);
		}
		String secret = Base64.getEncoder().encodeToString(keyBytes);
		JwtUtil jwtUtil = new JwtUtil(secret);

		SecretKey key = Keys.hmacShaKeyFor(keyBytes);
		String token = Jwts.builder()
				.claim("id", 99L)
				.claim("role", "USER")
				.setExpiration(new Date(System.currentTimeMillis() - 60_000))
				.signWith(key)
				.compact();

		assertThatThrownBy(() -> jwtUtil.isExpired(token))
				.isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
	}
}
