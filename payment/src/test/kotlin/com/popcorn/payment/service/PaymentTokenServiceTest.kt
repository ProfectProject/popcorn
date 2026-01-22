package com.popcorn.payment.service

import com.popcorn.payment.exception.PaymentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PaymentTokenService 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - AES/GCM 암호화 기반 결제 토큰 생성/복호화 테스트
 * - 토큰 만료 시간 검증 테스트
 * - 토큰 유효성 검증 및 예외 처리 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentTokenServiceTest {

    private val paymentTokenService = PaymentTokenService(
        secret = "test_secret_key_for_payment_token_service_testing_12345678",
        ttlMinutes = 30
    )

    private val shortTtlService = PaymentTokenService(
        secret = "test_secret_key_for_payment_token_service_testing_12345678",
        ttlMinutes = -1  // 음수로 설정하여 즉시 만료되도록 함
    )

    @Test
    fun `토큰 생성 성공 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 15000

        // When
        val token = paymentTokenService.generate(orderId, amount)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.matches(Regex("[A-Za-z0-9_-]+"))) // Base64 URL-safe 형식 확인
    }

    @Test
    fun `토큰 생성 및 복호화 성공 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 25000

        // When
        val token = paymentTokenService.generate(orderId, amount)
        val payload = paymentTokenService.decode(token)

        // Then
        assertEquals(orderId, payload.orderId)
        assertEquals(amount, payload.amount)
        assertTrue(payload.issuedAtMillis > 0)
        assertTrue(payload.issuedAtMillis <= Instant.now().toEpochMilli())
    }

    @Test
    fun `다양한 금액으로 토큰 생성 테스트`() {
        val amounts = listOf(1000, 50000, 100000, 999999)
        val orderId = UUID.randomUUID()

        amounts.forEach { amount ->
            // When
            val token = paymentTokenService.generate(orderId, amount)
            val payload = paymentTokenService.decode(token)

            // Then
            assertEquals(orderId, payload.orderId)
            assertEquals(amount, payload.amount)
        }
    }

    @Test
    fun `다양한 주문ID로 토큰 생성 테스트`() {
        val orderIds = listOf(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        )
        val amount = 10000

        orderIds.forEach { orderId ->
            // When
            val token = paymentTokenService.generate(orderId, amount)
            val payload = paymentTokenService.decode(token)

            // Then
            assertEquals(orderId, payload.orderId)
            assertEquals(amount, payload.amount)
        }
    }

    @Test
    fun `토큰 복호화 시 발급 시간 검증 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 15000
        val beforeGeneration = Instant.now().toEpochMilli()

        // When
        val token = paymentTokenService.generate(orderId, amount)
        val afterGeneration = Instant.now().toEpochMilli()
        val payload = paymentTokenService.decode(token)

        // Then
        assertTrue(payload.issuedAtMillis >= beforeGeneration)
        assertTrue(payload.issuedAtMillis <= afterGeneration)
    }

    @Test
    fun `잘못된 토큰 형식 복호화 실패 테스트`() {
        // Given
        val invalidTokens = listOf(
            "invalid_token",
            "",
            "abc123",
            "SGVsbG8gV29ybGQ",  // 유효한 Base64이지만 잘못된 형식
            "12345"
        )

        invalidTokens.forEach { invalidToken ->
            // When & Then
            assertThrows<PaymentException.InvalidRequest> {
                paymentTokenService.decode(invalidToken)
            }
        }
    }

    @Test
    fun `너무 짧은 토큰 복호화 실패 테스트`() {
        // Given
        val shortToken = "YWJjZGU"  // 12바이트보다 짧은 토큰

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            paymentTokenService.decode(shortToken)
        }
    }

    @Test
    fun `만료된 토큰 복호화 실패 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 10000

        // When
        val token = shortTtlService.generate(orderId, amount)

        // 충분히 대기하여 토큰이 만료되도록 함
        Thread.sleep(1000)

        // Then
        assertThrows<PaymentException.InvalidRequest> {
            shortTtlService.decode(token)
        }
    }

    @Test
    fun `잘못된 Base64 토큰 복호화 실패 테스트`() {
        // Given
        val invalidBase64Tokens = listOf(
            "invalid!@#$%",
            "토큰테스트",
            "invalid base64 with spaces"
        )

        invalidBase64Tokens.forEach { invalidToken ->
            // When & Then
            assertThrows<PaymentException.InvalidRequest> {
                paymentTokenService.decode(invalidToken)
            }
        }
    }

    @Test
    fun `서로 다른 secret key로 생성된 토큰 복호화 실패 테스트`() {
        // Given
        val differentSecretService = PaymentTokenService(
            secret = "different_secret_key_for_testing_9876543210",
            ttlMinutes = 30
        )

        val orderId = UUID.randomUUID()
        val amount = 15000

        // When
        val tokenFromOriginal = paymentTokenService.generate(orderId, amount)

        // Then
        assertThrows<PaymentException.InvalidRequest> {
            differentSecretService.decode(tokenFromOriginal)
        }
    }

    @Test
    fun `PaymentTokenPayload 데이터 클래스 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 20000
        val issuedAt = Instant.now().toEpochMilli()

        // When
        val payload1 = PaymentTokenPayload(orderId, amount, issuedAt)
        val payload2 = PaymentTokenPayload(orderId, amount, issuedAt)
        val payload3 = PaymentTokenPayload(UUID.randomUUID(), amount, issuedAt)

        // Then
        assertEquals(orderId, payload1.orderId)
        assertEquals(amount, payload1.amount)
        assertEquals(issuedAt, payload1.issuedAtMillis)

        // equals 및 hashCode 테스트
        assertEquals(payload1, payload2)
        assertEquals(payload1.hashCode(), payload2.hashCode())
        assertTrue(payload1 != payload3)
        assertTrue(payload1.hashCode() != payload3.hashCode())

        // toString 테스트
        assertTrue(payload1.toString().contains(orderId.toString()))
        assertTrue(payload1.toString().contains(amount.toString()))
        assertTrue(payload1.toString().contains(issuedAt.toString()))
    }

    @Test
    fun `PaymentTokenPayload copy 메소드 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 30000
        val issuedAt = Instant.now().toEpochMilli()
        val original = PaymentTokenPayload(orderId, amount, issuedAt)

        // When
        val newOrderId = UUID.randomUUID()
        val copied = original.copy(orderId = newOrderId, amount = 50000)

        // Then
        assertEquals(newOrderId, copied.orderId)
        assertEquals(50000, copied.amount)
        assertEquals(issuedAt, copied.issuedAtMillis)  // 변경되지 않은 필드

        // 원본 불변성 확인
        assertEquals(orderId, original.orderId)
        assertEquals(amount, original.amount)
    }

    @Test
    fun `동일한 매개변수로 생성된 토큰은 서로 다름 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 15000

        // When - 동일한 매개변수로 여러 토큰 생성
        val token1 = paymentTokenService.generate(orderId, amount)
        val token2 = paymentTokenService.generate(orderId, amount)
        val token3 = paymentTokenService.generate(orderId, amount)

        // Then - 각 토큰은 서로 달라야 함 (서로 다른 IV 때문)
        assertTrue(token1 != token2)
        assertTrue(token1 != token3)
        assertTrue(token2 != token3)

        // 하지만 복호화 결과는 동일해야 함
        val payload1 = paymentTokenService.decode(token1)
        val payload2 = paymentTokenService.decode(token2)
        val payload3 = paymentTokenService.decode(token3)

        assertEquals(payload1.orderId, payload2.orderId)
        assertEquals(payload1.amount, payload2.amount)
        assertEquals(payload1.orderId, payload3.orderId)
        assertEquals(payload1.amount, payload3.amount)
    }

    @Test
    fun `토큰 생성 시 예외상황 처리 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val negativeAmount = -1000

        // When & Then - 음수 금액도 처리 가능해야 함 (비즈니스 로직에 따라 다름)
        val token = paymentTokenService.generate(orderId, negativeAmount)
        val payload = paymentTokenService.decode(token)

        assertEquals(orderId, payload.orderId)
        assertEquals(negativeAmount, payload.amount)
    }
}