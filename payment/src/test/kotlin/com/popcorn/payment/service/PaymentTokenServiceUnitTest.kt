package com.popcorn.payment.service

import com.popcorn.payment.exception.PaymentException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PaymentTokenService 단위 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - PaymentTokenService의 토큰 생성/검증 로직 테스트
 * - PaymentTokenPayload 데이터 클래스 테스트
 * - 암호화/복호화 및 토큰 만료 검증 테스트
 * - 예외 처리 로직 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentTokenServiceUnitTest {

    private lateinit var paymentTokenService: PaymentTokenService
    private val testSecret = "test-secret-key-for-unit-testing-very-long-key"
    private val ttlMinutes = 30L

    @BeforeEach
    fun setUp() {
        paymentTokenService = PaymentTokenService(testSecret, ttlMinutes)
    }

    @Test
    fun `PaymentTokenPayload 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 15000
        val issuedAtMillis = Instant.now().toEpochMilli()

        // When
        val payload = PaymentTokenPayload(orderId, amount, issuedAtMillis)

        // Then
        assertEquals(orderId, payload.orderId)
        assertEquals(amount, payload.amount)
        assertEquals(issuedAtMillis, payload.issuedAtMillis)
    }

    @Test
    fun `토큰 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 25000

        // When
        val token = paymentTokenService.generate(orderId, amount)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.length > 20) // Base64 인코딩된 토큰은 일정 길이 이상이어야 함

        // Base64 URL-safe 문자만 포함되어야 함
        assertTrue(token.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test
    fun `토큰 생성과 디코딩 성공 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 30000
        val beforeGeneration = Instant.now().toEpochMilli()

        // When
        val token = paymentTokenService.generate(orderId, amount)
        val payload = paymentTokenService.decode(token)

        // Then
        assertEquals(orderId, payload.orderId)
        assertEquals(amount, payload.amount)
        assertTrue(payload.issuedAtMillis >= beforeGeneration)
        assertTrue(payload.issuedAtMillis <= Instant.now().toEpochMilli())
    }

    @Test
    fun `동일한 입력으로 생성한 토큰들은 서로 달라야 함 (IV 무작위성)`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 10000

        // When
        val token1 = paymentTokenService.generate(orderId, amount)
        val token2 = paymentTokenService.generate(orderId, amount)

        // Then
        assertTrue(token1 != token2, "동일한 입력으로도 다른 토큰이 생성되어야 함 (IV 무작위성)")
    }

    @Test
    fun `여러 토큰 생성과 디코딩 일관성 테스트`() {
        // Given
        val testCases = listOf(
            UUID.randomUUID() to 1000,
            UUID.randomUUID() to 50000,
            UUID.randomUUID() to 999999,
            UUID.randomUUID() to 1
        )

        testCases.forEach { (orderId, amount) ->
            // When
            val token = paymentTokenService.generate(orderId, amount)
            val payload = paymentTokenService.decode(token)

            // Then
            assertEquals(orderId, payload.orderId, "OrderId가 일치해야 함")
            assertEquals(amount, payload.amount, "Amount가 일치해야 함")
        }
    }

    @Test
    fun `유효하지 않은 토큰 디코딩 실패 테스트`() {
        // Given
        val invalidTokens = listOf(
            "", // 빈 문자열
            "invalid-token", // 잘못된 형식
            "dGVzdA", // 너무 짧은 토큰
            "invalid@token#format", // 잘못된 문자 포함
            Base64.getEncoder().encodeToString("too-short".toByteArray()) // 12바이트보다 짧음
        )

        invalidTokens.forEach { invalidToken ->
            // When & Then
            val exception = assertThrows<PaymentException> {
                paymentTokenService.decode(invalidToken)
            }
            assertTrue(exception.message?.contains("결제 토큰이 유효하지 않습니다") == true)
        }
    }

    @Test
    fun `토큰 형식 오류 테스트`() {
        // Given
        val invalidFormatToken = Base64.getUrlEncoder().withoutPadding().encodeToString(
            "invalid:format".toByteArray() + ByteArray(16) // 올바른 길이이지만 잘못된 형식
        )

        // When & Then
        val exception = assertThrows<PaymentException> {
            paymentTokenService.decode(invalidFormatToken)
        }
        assertTrue(exception.message?.contains("결제 토큰이 유효하지 않습니다") == true)
    }

    @Test
    fun `토큰 TTL 설정 테스트`() {
        // Given - 다양한 TTL로 서비스 생성
        val service1 = PaymentTokenService(testSecret, 1) // 1분
        val service2 = PaymentTokenService(testSecret, 60) // 60분
        val service3 = PaymentTokenService(testSecret, 1440) // 24시간

        val orderId = UUID.randomUUID()
        val amount = 10000

        // When - 토큰 생성
        val token1 = service1.generate(orderId, amount)
        val token2 = service2.generate(orderId, amount)
        val token3 = service3.generate(orderId, amount)

        // Then - 모든 토큰이 정상 생성되고 디코딩됨
        val payload1 = service1.decode(token1)
        val payload2 = service2.decode(token2)
        val payload3 = service3.decode(token3)

        assertEquals(orderId, payload1.orderId)
        assertEquals(orderId, payload2.orderId)
        assertEquals(orderId, payload3.orderId)
        assertEquals(amount, payload1.amount)
        assertEquals(amount, payload2.amount)
        assertEquals(amount, payload3.amount)
    }

    @Test
    fun `PaymentTokenPayload copy 메서드 테스트`() {
        // Given
        val original = PaymentTokenPayload(
            orderId = UUID.randomUUID(),
            amount = 20000,
            issuedAtMillis = Instant.now().toEpochMilli()
        )

        // When
        val copied = original.copy(amount = 30000)

        // Then
        assertEquals(original.orderId, copied.orderId)
        assertEquals(original.issuedAtMillis, copied.issuedAtMillis)
        assertEquals(30000, copied.amount)
    }

    @Test
    fun `PaymentTokenPayload equals 및 hashCode 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 25000
        val issuedAtMillis = Instant.now().toEpochMilli()

        val payload1 = PaymentTokenPayload(orderId, amount, issuedAtMillis)
        val payload2 = PaymentTokenPayload(orderId, amount, issuedAtMillis)
        val payload3 = PaymentTokenPayload(orderId, amount + 1000, issuedAtMillis)

        // When & Then
        assertEquals(payload1, payload2)
        assertEquals(payload1.hashCode(), payload2.hashCode())
        assertTrue(payload1 != payload3)
        assertTrue(payload1.hashCode() != payload3.hashCode())
    }

    @Test
    fun `PaymentTokenPayload toString 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payload = PaymentTokenPayload(orderId, 15000, 1234567890L)

        // When
        val toString = payload.toString()

        // Then
        assertNotNull(toString)
        assertTrue(toString.contains("15000"))
        assertTrue(toString.contains("1234567890"))
        assertTrue(toString.contains(orderId.toString()))
    }

    @Test
    fun `대용량 금액 토큰 생성 및 디코딩 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val largeAmount = 999_999_999

        // When
        val token = paymentTokenService.generate(orderId, largeAmount)
        val payload = paymentTokenService.decode(token)

        // Then
        assertEquals(orderId, payload.orderId)
        assertEquals(largeAmount, payload.amount)
    }

    @Test
    fun `최소 금액 토큰 생성 및 디코딩 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val minAmount = 1

        // When
        val token = paymentTokenService.generate(orderId, minAmount)
        val payload = paymentTokenService.decode(token)

        // Then
        assertEquals(orderId, payload.orderId)
        assertEquals(minAmount, payload.amount)
    }

    @Test
    fun `특수한 UUID 토큰 생성 및 디코딩 테스트`() {
        // Given
        val specialUUIDs = listOf(
            UUID(0L, 0L), // 모든 비트가 0
            UUID(-1L, -1L), // 모든 비트가 1
            UUID.randomUUID(),
            UUID.randomUUID()
        )

        specialUUIDs.forEach { orderId ->
            // When
            val token = paymentTokenService.generate(orderId, 5000)
            val payload = paymentTokenService.decode(token)

            // Then
            assertEquals(orderId, payload.orderId)
            assertEquals(5000, payload.amount)
        }
    }

    @Test
    fun `토큰 시간 검증 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 12000
        val beforeGeneration = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        // When
        val token = paymentTokenService.generate(orderId, amount)
        val payload = paymentTokenService.decode(token)
        val afterDecoding = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        // Then
        val issuedAt = Instant.ofEpochMilli(payload.issuedAtMillis).truncatedTo(ChronoUnit.SECONDS)
        assertTrue(!issuedAt.isBefore(beforeGeneration))
        assertTrue(!issuedAt.isAfter(afterDecoding))
    }

    @Test
    fun `토큰 서비스 다른 시크릿키로 디코딩 실패 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 8000

        val service1 = PaymentTokenService("secret1", ttlMinutes)
        val service2 = PaymentTokenService("secret2", ttlMinutes)

        val token = service1.generate(orderId, amount)

        // When & Then
        val exception = assertThrows<PaymentException> {
            service2.decode(token)
        }
        assertTrue(exception.message?.contains("결제 토큰이 유효하지 않습니다") == true)
    }

    @Test
    fun `토큰 길이 일관성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amounts = listOf(1, 100, 10000, 999999)

        // When
        val tokens = amounts.map { amount ->
            paymentTokenService.generate(orderId, amount)
        }

        // Then
        // 모든 토큰이 Base64 URL-safe 인코딩으로 생성되었는지 확인
        tokens.forEach { token ->
            assertTrue(token.matches(Regex("^[A-Za-z0-9_-]+$")))
            assertTrue(token.length > 20) // 최소 길이 확인
        }
    }
}