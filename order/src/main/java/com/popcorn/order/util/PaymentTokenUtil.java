package com.popcorn.order.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

/**
 * 결제 토큰 JWT 암호화/복호화 유틸리티 (기존 backend 호환)
 *
 * JWT HS256 방식으로 결제 정보를 안전하게 암호화하여 토큰 생성
 * 기존 backend PaymentTokenService와 동일한 방식 사용
 */
@Slf4j
@Component
public class PaymentTokenUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long TOKEN_VALIDITY_MINUTES = 30; // 30분 유효

    /**
     * 결제 정보를 JWT 토큰으로 암호화 (기존 backend 호환)
     */
    public String generatePaymentToken(UUID orderId, String orderNo, int amount, String orderName,
                                     String customerKey, String paymentMethod) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + TOKEN_VALIDITY_MINUTES * 60 * 1000);

            String token = Jwts.builder()
                    .setSubject("p") // "payment" → "p" (더 짧게)
                    .setIssuedAt(now)
                    .setExpiration(expiry)
                    .claim("i", orderId.toString()) // "orderId" → "i"
                    .claim("o", orderNo) // "orderNo" → "o"
                    .claim("a", amount) // "amount" → "a"
                    .claim("c", customerKey) // "customerKey" → "c"
                    .claim("p", null) // "paymentId" → "p" (아직 결제 기록 없음)
                    // successUrl, failUrl 제거 (프론트엔드에서 설정)
                    .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                    .compact();

            log.info("💳 JWT 결제 토큰 생성 완료 - 주문번호: {}, 토큰 길이: {}", orderNo, token.length());
            return token;

        } catch (Exception e) {
            log.error("❌ JWT 결제 토큰 생성 실패 - 주문번호: {}, 에러: {}", orderNo, e.getMessage(), e);
            throw new RuntimeException("결제 토큰 생성에 실패했습니다", e);
        }
    }

    /**
     * JWT 토큰에서 결제 정보 복호화 (테스트/디버깅용)
     */
    public PaymentTokenInfo decryptPaymentToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String orderIdValue = claims.get("i", String.class);
            UUID orderId = null;
            if (orderIdValue != null && !orderIdValue.isBlank()) {
                orderId = UUID.fromString(orderIdValue);
            }

            String paymentIdValue = claims.get("p", String.class);
            UUID paymentId = null;
            if (paymentIdValue != null && !paymentIdValue.isBlank()) {
                paymentId = UUID.fromString(paymentIdValue);
            }

            PaymentTokenInfo info = PaymentTokenInfo.builder()
                    .orderId(orderId)
                    .orderNo(claims.get("o", String.class))
                    .amount(claims.get("a", Integer.class))
                    .customerKey(claims.get("c", String.class))
                    .paymentId(paymentId)
                    // URL은 프론트엔드에서 하드코딩으로 처리
                    .successUrl("http://localhost:3000/payments/success")
                    .failUrl("http://localhost:3000/payments/fail")
                    .build();

            log.info("✅ JWT 결제 토큰 복호화 완료 - 주문번호: {}", info.getOrderNo());
            return info;

        } catch (Exception e) {
            String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
            log.error("❌ JWT 결제 토큰 복호화 실패 - 토큰: {}, 에러: {}", tokenPreview, e.getMessage());
            throw new RuntimeException("결제 토큰 복호화에 실패했습니다", e);
        }
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean isValidToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("⚠️ JWT 토큰 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 결제 토큰 정보 DTO (기존 backend 호환)
     */
    @lombok.Builder
    @lombok.Getter
    public static class PaymentTokenInfo {
        private UUID orderId;
        private String orderNo;
        private Integer amount;
        private String customerKey;
        private UUID paymentId;
        private String successUrl;
        private String failUrl;
    }
}