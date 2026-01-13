package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Builder;
import lombok.Getter;

@Service
public class PaymentTokenService {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long TOKEN_VALIDITY_MINUTES = 30; // 30분 유효

    /**
     * 결제 정보를 JWT 토큰으로 암호화
     */
    public String createPaymentToken(PaymentTokenInfo paymentInfo) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TOKEN_VALIDITY_MINUTES * 60 * 1000);

        return Jwts.builder()
                .setSubject("payment")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("orderNo", paymentInfo.getOrderNo())
                .claim("amount", paymentInfo.getAmount())
                .claim("customerKey", paymentInfo.getCustomerKey())
                .claim("paymentId", paymentInfo.getPaymentId().toString())
                .claim("successUrl", paymentInfo.getSuccessUrl())
                .claim("failUrl", paymentInfo.getFailUrl())
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();
    }

    /**
     * JWT 토큰에서 결제 정보 복호화
     */
    public PaymentTokenInfo parsePaymentToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            return PaymentTokenInfo.builder()
                    .orderNo(claims.get("orderNo", String.class))
                    .amount(claims.get("amount", Integer.class))
                    .customerKey(claims.get("customerKey", String.class))
                    .paymentId(UUID.fromString(claims.get("paymentId", String.class)))
                    .successUrl(claims.get("successUrl", String.class))
                    .failUrl(claims.get("failUrl", String.class))
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 결제 토큰입니다.", e);
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isValidToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes())
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Getter
    @Builder
    public static class PaymentTokenInfo {
        private String orderNo;
        private Integer amount;
        private String customerKey;
        private UUID paymentId;
        private String successUrl;
        private String failUrl;
    }
}