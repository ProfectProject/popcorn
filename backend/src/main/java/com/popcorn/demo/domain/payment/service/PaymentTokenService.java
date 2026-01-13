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
                .setSubject("p") // "payment" → "p" (더 짧게)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("o", paymentInfo.getOrderNo()) // "orderNo" → "o"
                .claim("a", paymentInfo.getAmount()) // "amount" → "a"
                .claim("c", paymentInfo.getCustomerKey()) // "customerKey" → "c"
                .claim("p", paymentInfo.getPaymentId().toString()) // "paymentId" → "p"
                // successUrl, failUrl 제거 (프론트엔드에서 설정)
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
                    .orderNo(claims.get("o", String.class)) // 축약된 필드명 사용
                    .amount(claims.get("a", Integer.class))
                    .customerKey(claims.get("c", String.class))
                    .paymentId(UUID.fromString(claims.get("p", String.class)))
                    // URL은 프론트엔드에서 하드코딩으로 처리
                    .successUrl("http://localhost:3000/payments/success")
                    .failUrl("http://localhost:3000/payments/fail")
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