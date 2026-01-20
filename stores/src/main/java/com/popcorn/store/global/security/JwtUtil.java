package com.popcorn.store.global.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	private final SecretKey secretKey;

	public JwtUtil(@Value("${jwt.secret}") String secret) {
		byte[] byteSecretKey = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(byteSecretKey);
	}

	public Long getUserId(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(secretKey)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.get("id", Long.class);
	}

	public String getRole(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(secretKey)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.get("role", String.class);
	}

	public boolean isExpired(String token) {
		Date expiration = Jwts.parserBuilder()
			.setSigningKey(secretKey)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.getExpiration();
		return expiration != null && expiration.before(new Date());
	}
}
