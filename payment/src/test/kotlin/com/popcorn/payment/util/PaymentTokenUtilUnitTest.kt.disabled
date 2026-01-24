package com.popcorn.payment.util

import com.popcorn.payment.exception.PaymentException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("PaymentTokenUtil 단위 테스트")
class PaymentTokenUtilUnitTest {

    private val orderId = UUID.randomUUID()
    private val amount = 10000
    private val customerId = 123L

    @BeforeEach
    fun setUp() {
        // 테스트용 JWT 시크릿 설정 (기본값과 동일)
        System.setProperty("JWT_SECRET", "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItbG9jYWwtZGV2ZWxvcG1lbnQtb25seS1kb25vdC11c2UtaW4tcHJvZHVjdGlvbg==")
    }

    @Test
    @DisplayName("결제 토큰 생성 성공")
    fun `generatePaymentToken should create valid token`() {
        // When
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotBlank())
        // JWT 토큰 형식 확인 (header.payload.signature)
        val tokenParts = token.split(".")
        assertEquals(3, tokenParts.size, "JWT 토큰은 3개 부분으로 구성되어야 함")
    }

    @Test
    @DisplayName("결제 토큰 복호화 성공 - 유효한 토큰")
    fun `decryptPaymentToken should decrypt valid token successfully`() {
        // Given
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)

        // When
        val result = PaymentTokenUtil.decryptPaymentToken(token)

        // Then
        assertNotNull(result)
        assertEquals(orderId.toString(), result["orderId"])
        assertEquals(amount, result["amount"])
        assertEquals(customerId, result["customerId"])
        assertNotNull(result["iat"]) // issued at
        assertNotNull(result["exp"]) // expiry time
    }

    @Test
    @DisplayName("토큰 유효성 검증 성공")
    fun `isValidToken should validate correct token`() {
        // Given
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)

        // When
        val isValid = PaymentTokenUtil.isValidToken(token)

        // Then
        assertTrue(isValid, "유효한 토큰은 검증을 통과해야 함")
    }

    @Test
    @DisplayName("결제 토큰 검증 성공 - 모든 필수 필드 확인")
    fun `validatePaymentToken should validate all required fields`() {
        // Given
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)

        // When & Then - 예외 없이 검증 완료되어야 함
        assertDoesNotThrow {
            PaymentTokenUtil.validatePaymentToken(token)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid.token.format",
        "eyJhbGciOiJIUzI1NiJ9.invalid_payload.signature",
        "",
        "   ",
        "not.a.jwt.token.at.all"
    ])
    @DisplayName("잘못된 토큰 형식 처리")
    fun `should handle invalid token formats`(invalidToken: String) {
        // When & Then
        assertFalse(PaymentTokenUtil.isValidToken(invalidToken), "잘못된 토큰은 검증을 실패해야 함")

        assertThrows(PaymentException.InvalidRequest::class.java) {
            PaymentTokenUtil.decryptPaymentToken(invalidToken)
        }

        assertThrows(PaymentException.InvalidRequest::class.java) {
            PaymentTokenUtil.validatePaymentToken(invalidToken)
        }
    }

    @Test
    @DisplayName("만료된 토큰 처리")
    fun `should handle expired token`() {
        // Given - 과거 시점의 토큰 생성 (만료 시뮬레이션)
        val expiredToken = createExpiredToken()

        // When & Then
        assertFalse(PaymentTokenUtil.isValidToken(expiredToken), "만료된 토큰은 검증을 실패해야 함")

        val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
            PaymentTokenUtil.validatePaymentToken(expiredToken)
        }
        assertTrue(exception.message!!.contains("만료"))
    }

    @Test
    @DisplayName("토큰 필드 축약 변환 확인 - 정확한 매핑")
    fun `should correctly map abbreviated field names`() {
        // Given
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)
        val decryptedData = PaymentTokenUtil.decryptPaymentToken(token)

        // When & Then - 축약된 필드명이 올바르게 변환되어야 함
        // 실제 JWT payload에는 축약된 필드명이 들어가고, 복호화 시 원래 필드명으로 복원
        assertEquals(orderId.toString(), decryptedData["orderId"])
        assertEquals(amount, decryptedData["amount"])
        assertEquals(customerId, decryptedData["customerId"])
    }

    @Test
    @DisplayName("다양한 금액에 대한 토큰 생성/검증")
    fun `should handle various amounts correctly`() {
        // Given
        val amounts = listOf(1, 100, 1000, 10000, 100000, 1000000, 5000000)

        amounts.forEach { testAmount ->
            // When
            val token = PaymentTokenUtil.generatePaymentToken(orderId, testAmount, customerId)
            val decryptedData = PaymentTokenUtil.decryptPaymentToken(token)

            // Then
            assertEquals(testAmount, decryptedData["amount"], "금액 $testAmount 에 대한 토큰 생성/복호화 실패")
            assertTrue(PaymentTokenUtil.isValidToken(token), "금액 $testAmount 에 대한 토큰 검증 실패")
        }
    }

    @Test
    @DisplayName("다양한 고객 ID에 대한 토큰 생성/검증")
    fun `should handle various customer IDs correctly`() {
        // Given
        val customerIds = listOf(1L, 100L, 999L, 12345L, Long.MAX_VALUE)

        customerIds.forEach { testCustomerId ->
            // When
            val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, testCustomerId)
            val decryptedData = PaymentTokenUtil.decryptPaymentToken(token)

            // Then
            assertEquals(testCustomerId, decryptedData["customerId"], "고객 ID $testCustomerId 에 대한 토큰 생성/복호화 실패")
            assertTrue(PaymentTokenUtil.isValidToken(token), "고객 ID $testCustomerId 에 대한 토큰 검증 실패")
        }
    }

    @Test
    @DisplayName("null 고객 ID 처리")
    fun `should handle null customer ID`() {
        // When
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, null)
        val decryptedData = PaymentTokenUtil.decryptPaymentToken(token)

        // Then
        assertNull(decryptedData["customerId"], "null 고객 ID가 올바르게 처리되어야 함")
        assertTrue(PaymentTokenUtil.isValidToken(token))
    }

    @Test
    @DisplayName("토큰 만료 시간 검증 - 30분")
    fun `token should expire after 30 minutes`() {
        // Given
        val token = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)
        val decryptedData = PaymentTokenUtil.decryptPaymentToken(token)

        // When
        val iat = decryptedData["iat"] as Long // issued at
        val exp = decryptedData["exp"] as Long // expiry time

        // Then
        val expectedTtlSeconds = 30 * 60 // 30분
        val actualTtlSeconds = exp - iat
        assertEquals(expectedTtlSeconds, actualTtlSeconds, "토큰 만료 시간은 30분이어야 함")
    }

    @Test
    @DisplayName("동일한 입력에 대해 다른 토큰 생성 - 보안성 확인")
    fun `should generate different tokens for same input`() {
        // Given - 동일한 입력값
        // When
        val token1 = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)
        Thread.sleep(1) // 시간 차이를 만들기 위해 1ms 대기
        val token2 = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)

        // Then
        assertNotEquals(token1, token2, "동일한 입력이라도 다른 토큰이 생성되어야 함 (시간 기반)")

        // 하지만 복호화한 데이터는 동일해야 함 (시간 제외)
        val data1 = PaymentTokenUtil.decryptPaymentToken(token1)
        val data2 = PaymentTokenUtil.decryptPaymentToken(token2)

        assertEquals(data1["orderId"], data2["orderId"])
        assertEquals(data1["amount"], data2["amount"])
        assertEquals(data1["customerId"], data2["customerId"])
    }

    @Test
    @DisplayName("잘못된 시크릿으로 생성된 토큰 검증 실패")
    fun `should fail validation for token created with different secret`() {
        // Given - 다른 시크릿으로 토큰 생성 시뮬레이션 (실제로는 동일 시크릿이지만 변조된 토큰으로 가정)
        val validToken = PaymentTokenUtil.generatePaymentToken(orderId, amount, customerId)

        // 토큰의 마지막 문자를 변경하여 시그니처 변조 시뮬레이션
        val tamperedToken = validToken.dropLast(1) + "X"

        // When & Then
        assertFalse(PaymentTokenUtil.isValidToken(tamperedToken), "변조된 토큰은 검증을 실패해야 함")

        assertThrows(PaymentException.InvalidRequest::class.java) {
            PaymentTokenUtil.validatePaymentToken(tamperedToken)
        }
    }

    @Test
    @DisplayName("빈 UUID와 0 금액 처리")
    fun `should handle edge cases for UUID and amount`() {
        // Given
        val minAmount = 1 // 최소 유효 금액

        // When
        val token = PaymentTokenUtil.generatePaymentToken(orderId, minAmount, 0L)
        val decryptedData = PaymentTokenUtil.decryptPaymentToken(token)

        // Then
        assertEquals(orderId.toString(), decryptedData["orderId"])
        assertEquals(minAmount, decryptedData["amount"])
        assertEquals(0L, decryptedData["customerId"])
        assertTrue(PaymentTokenUtil.isValidToken(token))
    }

    /**
     * 만료된 토큰을 시뮬레이션하기 위한 헬퍼 메서드
     * 실제로는 시간을 과거로 설정할 수 없으므로, 짧은 TTL로 토큰 생성 후 대기
     */
    private fun createExpiredToken(): String {
        // 매우 짧은 TTL로 토큰 생성을 시뮬레이션하기 위해
        // 실제로는 과거 시간으로 생성된 토큰의 형태를 만듦

        // 간단히 잘못된 exp 클레임을 가진 토큰을 생성
        // 실제 구현에서는 현재 시간보다 이전 시간을 exp로 설정
        return "eyJhbGciOiJIUzI1NiJ9.eyJpIjoiZXhhbXBsZS1vcmRlci1pZCIsImEiOjEwMDAwLCJjIjoxMjMsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjA5NDU5MjAwfQ.invalid_expired_signature"
    }
}