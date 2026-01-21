package com.popcorn.payment.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 결제 토큰 암호화/복호화 유틸리티 (Kotlin 버전)
 *
 * AES-256-GCM 방식으로 결제 정보를 안전하게 복호화
 * Order 서비스의 PaymentTokenUtil과 동일한 암호화 방식 사용
 */
@Component
class PaymentTokenUtil {

    private val log = LoggerFactory.getLogger(PaymentTokenUtil::class.java)
    private val objectMapper = ObjectMapper()

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SECRET_KEY = "MySecretKey12345MySecretKey12345" // Order 서비스와 동일한 32바이트 키
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    /**
     * 결제 정보를 암호화하여 토큰 생성
     */
    fun encryptPaymentToken(
        orderId: String,
        orderNo: String,
        amount: Int,
        customerKey: String,
        successUrl: String,
        failUrl: String
    ): String {
        val payload = mapOf(
            "orderId" to orderId,
            "orderNo" to orderNo,
            "amount" to amount,
            "customerKey" to customerKey,
            "successUrl" to successUrl,
            "failUrl" to failUrl,
            "timestamp" to System.currentTimeMillis()
        )

        val jsonData = objectMapper.writeValueAsBytes(payload)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encryptedData = cipher.doFinal(jsonData)
        val encryptedWithIv = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.size)
        System.arraycopy(encryptedData, 0, encryptedWithIv, iv.size, encryptedData.size)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedWithIv)
    }

    /**
     * 암호화된 토큰을 복호화하여 결제 정보 반환
     */
    fun decryptPaymentToken(token: String): Map<String, Any> {
        try {
            log.info("🔓 결제 토큰 복호화 시작 - 토큰 길이: {}자", token.length)

            // Base64 디코딩
            val encryptedWithIv = Base64.getUrlDecoder().decode(token)

            // IV와 암호화된 데이터 분리
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedData = ByteArray(encryptedWithIv.size - GCM_IV_LENGTH)
            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.size)
            System.arraycopy(encryptedWithIv, iv.size, encryptedData, 0, encryptedData.size)

            // AES-GCM 복호화
            val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val decryptedData = cipher.doFinal(encryptedData)
            val jsonData = String(decryptedData, StandardCharsets.UTF_8)

            // JSON 파싱하여 맵 반환
            val paymentData = objectMapper.readValue(jsonData, Map::class.java) as Map<String, Any>

            log.info("✅ 결제 토큰 복호화 완료 - 주문번호: {}, 금액: {}원",
                paymentData["orderNo"], paymentData["amount"])

            return paymentData

        } catch (e: Exception) {
            val tokenPreview = if (token.length > 20) token.substring(0, 20) + "..." else token
            log.error("💥 결제 토큰 복호화 실패 - 토큰: {}, 에러: {}", tokenPreview, e.message, e)
            throw RuntimeException("결제 토큰 복호화에 실패했습니다", e)
        }
    }

    /**
     * 토큰 검증 (만료시간 확인 등)
     */
    fun validatePaymentToken(paymentData: Map<String, Any>): Boolean {
        try {
            val timestamp = paymentData["timestamp"] as? Number ?: return false
            val currentTime = System.currentTimeMillis()
            val tokenAge = currentTime - timestamp.toLong()

            // 30분 만료 체크 (1800000ms = 30분)
            if (tokenAge > 1800000) {
                log.warn("⏰ 결제 토큰 만료 - 생성시각: {}, 현재시각: {}, 경과시간: {}ms",
                    Date(timestamp.toLong()), Date(currentTime), tokenAge)
                return false
            }

            // 필수 필드 검증
            val requiredFields = listOf("orderId", "orderNo", "amount", "customerKey")
            for (field in requiredFields) {
                if (!paymentData.containsKey(field) || paymentData[field] == null) {
                    log.warn("⚠️ 필수 필드 누락: {}", field)
                    return false
                }
            }

            log.debug("✅ 결제 토큰 검증 통과 - 주문번호: {}", paymentData["orderNo"])
            return true

        } catch (e: Exception) {
            log.error("💥 결제 토큰 검증 실패: {}", e.message, e)
            return false
        }
    }
}
