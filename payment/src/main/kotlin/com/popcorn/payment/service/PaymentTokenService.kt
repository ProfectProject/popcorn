package com.popcorn.payment.service

import com.popcorn.payment.exception.PaymentException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class PaymentTokenService(
    @Value("\${payment.token.secret:change-me}") secret: String,
    @Value("\${payment.token.ttl-minutes:30}") private val ttlMinutes: Long
) {

    private val secretKey = SecretKeySpec(sha256(secret), "AES")
    private val secureRandom = SecureRandom()

    fun generate(orderId: UUID, amount: Int): String {
        val issuedAt = Instant.now().toEpochMilli()
        val payload = "$orderId:$amount:$issuedAt"
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(payload.toByteArray(StandardCharsets.UTF_8))

        val tokenBytes = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, tokenBytes, 0, iv.size)
        System.arraycopy(cipherText, 0, tokenBytes, iv.size, cipherText.size)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
    }

    fun decode(token: String): PaymentTokenPayload {
        try {
            val decoded = Base64.getUrlDecoder().decode(token)
            if (decoded.size <= 12) {
                throw PaymentException.invalidRequest("결제 토큰이 유효하지 않습니다.")
            }

            val iv = decoded.copyOfRange(0, 12)
            val cipherText = decoded.copyOfRange(12, decoded.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(cipherText)
            val parts = String(plain, StandardCharsets.UTF_8).split(":")
            if (parts.size != 3) {
                throw PaymentException.invalidRequest("결제 토큰 형식이 올바르지 않습니다.")
            }

            val orderId = UUID.fromString(parts[0])
            val amount = parts[1].toInt()
            val issuedAt = parts[2].toLong()

            val age = Duration.between(Instant.ofEpochMilli(issuedAt), Instant.now())
            if (age.toMinutes() > ttlMinutes) {
                throw PaymentException.invalidRequest("결제 토큰이 만료되었습니다.")
            }

            return PaymentTokenPayload(orderId, amount, issuedAt)
        } catch (e: PaymentException) {
            throw e
        } catch (e: Exception) {
            throw PaymentException.invalidRequest("결제 토큰이 유효하지 않습니다.")
        }
    }

    private fun sha256(value: String): ByteArray {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
    }
}

data class PaymentTokenPayload(
    val orderId: UUID,
    val amount: Int,
    val issuedAtMillis: Long
)
