package com.popcorn.order.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 결제 토큰 암호화/복호화 유틸리티
 *
 * AES-256-GCM 방식으로 결제 정보를 안전하게 암호화하여 토큰 생성
 */
@Slf4j
@Component
public class PaymentTokenUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SECRET_KEY = "MySecretKey12345MySecretKey12345"; // 32바이트 키
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 결제 정보를 암호화하여 토큰 생성
     */
    public String generatePaymentToken(UUID orderId, String orderNo, int amount, String orderName,
                                     String customerKey, String paymentMethod) {
        try {
            // 결제 정보 맵 생성
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("orderId", orderId.toString());
            paymentData.put("orderNo", orderNo);
            paymentData.put("amount", amount);
            paymentData.put("orderName", orderName);
            paymentData.put("customerKey", customerKey);
            paymentData.put("paymentMethod", paymentMethod);
            paymentData.put("timestamp", System.currentTimeMillis());
            paymentData.put("successUrl", "http://localhost:3000/payments/success");
            paymentData.put("failUrl", "http://localhost:3000/payments/fail");

            // JSON 문자열로 변환
            String jsonData = objectMapper.writeValueAsString(paymentData);

            // AES-GCM 암호화
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 랜덤 IV 생성
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // 암호화 수행
            byte[] encryptedData = cipher.doFinal(jsonData.getBytes(StandardCharsets.UTF_8));

            // IV + 암호화된 데이터를 Base64로 인코딩
            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);

            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedWithIv);

            log.info("결제 토큰 생성 완료 - 주문번호: {}, 토큰 길이: {}", orderNo, token.length());

            return token;

        } catch (Exception e) {
            log.error("결제 토큰 생성 실패 - 주문번호: {}, 에러: {}", orderNo, e.getMessage(), e);
            throw new RuntimeException("결제 토큰 생성에 실패했습니다", e);
        }
    }

    /**
     * 토큰 검증용 메서드 (테스트/디버깅용)
     */
    public Map<String, Object> decryptPaymentToken(String token) {
        try {
            // Base64 디코딩
            byte[] encryptedWithIv = Base64.getUrlDecoder().decode(token);

            // IV와 암호화된 데이터 분리
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedWithIv, iv.length, encryptedData, 0, encryptedData.length);

            // AES-GCM 복호화
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decryptedData = cipher.doFinal(encryptedData);
            String jsonData = new String(decryptedData, StandardCharsets.UTF_8);

            // JSON 파싱하여 맵 반환
            return objectMapper.readValue(jsonData, Map.class);

        } catch (Exception e) {
            log.error("결제 토큰 복호화 실패 - 토큰: {}, 에러: {}",
                    token.length() > 20 ? token.substring(0, 20) + "..." : token, e.getMessage());
            throw new RuntimeException("결제 토큰 복호화에 실패했습니다", e);
        }
    }
}