package com.popcorn.payment.exception

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PaymentException 포괄적 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 모든 PaymentException 하위 클래스 테스트
 * - 모든 팩토리 메서드 테스트
 * - 메시지 및 원인 예외 처리 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentExceptionAdvancedTest {

    @Test
    fun `PaymentNotFound 기본 생성 테스트`() {
        // When
        val exception = PaymentException.PaymentNotFound()

        // Then
        assertEquals("결제 정보를 찾을 수 없습니다.", exception.message)
        assertTrue(exception is PaymentException)
    }

    @Test
    fun `PaymentNotFound 커스텀 메시지 생성 테스트`() {
        // Given
        val customMessage = "특정 결제 정보를 찾을 수 없습니다."

        // When
        val exception = PaymentException.PaymentNotFound(customMessage)

        // Then
        assertEquals(customMessage, exception.message)
    }

    @Test
    fun `PaymentNotFound create 팩토리 메서드 테스트`() {
        // When
        val exception = PaymentException.PaymentNotFound.create()

        // Then
        assertEquals("결제 정보를 찾을 수 없습니다.", exception.message)
        assertTrue(exception is PaymentException.PaymentNotFound)
    }

    @Test
    fun `InvalidRequest 기본 생성 테스트`() {
        // When
        val exception = PaymentException.InvalidRequest()

        // Then
        assertEquals("잘못된 요청입니다.", exception.message)
        assertTrue(exception is PaymentException)
    }

    @Test
    fun `InvalidRequest 커스텀 메시지 생성 테스트`() {
        // Given
        val customMessage = "결제 금액이 올바르지 않습니다."

        // When
        val exception = PaymentException.InvalidRequest(customMessage)

        // Then
        assertEquals(customMessage, exception.message)
    }

    @Test
    fun `InvalidRequest create 팩토리 메서드들 테스트`() {
        // When
        val exception1 = PaymentException.InvalidRequest.create()
        val exception2 = PaymentException.InvalidRequest.create("특정 오류 메시지")

        // Then
        assertEquals("잘못된 요청입니다.", exception1.message)
        assertEquals("특정 오류 메시지", exception2.message)
        assertTrue(exception1 is PaymentException.InvalidRequest)
        assertTrue(exception2 is PaymentException.InvalidRequest)
    }

    @Test
    fun `PaymentExpired 기본 생성 테스트`() {
        // When
        val exception = PaymentException.PaymentExpired()

        // Then
        assertEquals("결제 가능 시간이 초과되었습니다.", exception.message)
        assertTrue(exception is PaymentException)
    }

    @Test
    fun `PaymentExpired 커스텀 메시지 생성 테스트`() {
        // Given
        val customMessage = "결제 세션이 만료되었습니다."

        // When
        val exception = PaymentException.PaymentExpired(customMessage)

        // Then
        assertEquals(customMessage, exception.message)
    }

    @Test
    fun `PaymentExpired create 팩토리 메서드 테스트`() {
        // When
        val exception = PaymentException.PaymentExpired.create()

        // Then
        assertEquals("결제 가능 시간이 초과되었습니다.", exception.message)
        assertTrue(exception is PaymentException.PaymentExpired)
    }

    @Test
    fun `PaymentException 정적 팩토리 메서드들 테스트`() {
        // When
        val paymentNotFound = PaymentException.paymentNotFound()
        val invalidRequest1 = PaymentException.invalidRequest()
        val invalidRequest2 = PaymentException.invalidRequest("커스텀 메시지")
        val duplicatePaymentAttempt = PaymentException.duplicatePaymentAttempt()
        val externalApiError1 = PaymentException.externalApiError("API 오류")
        val externalApiError2 = PaymentException.externalApiError("API 오류", RuntimeException("원인"))
        val invalidStatusTransition1 = PaymentException.invalidStatusTransition()
        val invalidStatusTransition2 = PaymentException.invalidStatusTransition("상태 전환 오류")
        val amountMismatch = PaymentException.amountMismatch()

        // Then
        assertTrue(paymentNotFound is PaymentException.PaymentNotFound)
        assertTrue(invalidRequest1 is PaymentException.InvalidRequest)
        assertTrue(invalidRequest2 is PaymentException.InvalidRequest)
        assertEquals("커스텀 메시지", invalidRequest2.message)

        assertNotNull(duplicatePaymentAttempt)
        assertNotNull(externalApiError1)
        assertNotNull(externalApiError2)
        assertNotNull(invalidStatusTransition1)
        assertNotNull(invalidStatusTransition2)
        assertNotNull(amountMismatch)
    }

    @Test
    fun `PaymentException 상속 구조 테스트`() {
        val exceptions = listOf(
            PaymentException.PaymentNotFound(),
            PaymentException.InvalidRequest(),
            PaymentException.PaymentExpired()
        )

        exceptions.forEach { exception ->
            assertTrue(exception is PaymentException)
            assertTrue(exception is RuntimeException)
            assertTrue(exception is Exception)
            assertTrue(exception is Throwable)
        }
    }

    @Test
    fun `PaymentException 메시지 null 처리 테스트`() {
        // Given
        val exceptions = listOf(
            PaymentException.PaymentNotFound(),
            PaymentException.InvalidRequest(),
            PaymentException.PaymentExpired()
        )

        // When & Then
        exceptions.forEach { exception ->
            assertNotNull(exception.message)
            assertTrue(exception.message!!.isNotBlank())
        }
    }

    @Test
    fun `PaymentException cause 처리 테스트`() {
        // Given
        val rootCause = IllegalArgumentException("근본 원인")

        // When
        val externalApiError = PaymentException.externalApiError("API 호출 실패", rootCause)

        // Then
        assertEquals("API 호출 실패", externalApiError.message)
        assertEquals(rootCause, externalApiError.cause)
    }

    @Test
    fun `다양한 예외 시나리오 테스트`() {
        val scenarios = mapOf(
            "결제키 없음" to PaymentException.paymentNotFound(),
            "잘못된 금액" to PaymentException.invalidRequest("금액이 0 이하입니다"),
            "중복 결제" to PaymentException.duplicatePaymentAttempt(),
            "외부 API 오류" to PaymentException.externalApiError("토스페이먼츠 API 오류"),
            "상태 전환 불가" to PaymentException.invalidStatusTransition("PAID에서 READY로 전환 불가"),
            "금액 불일치" to PaymentException.amountMismatch()
        )

        scenarios.forEach { (scenario, exception) ->
            assertNotNull(exception, "시나리오 '$scenario'에서 예외가 null입니다")
            assertTrue(exception is PaymentException, "시나리오 '$scenario'에서 PaymentException이 아닙니다")
            assertNotNull(exception.message, "시나리오 '$scenario'에서 메시지가 null입니다")
        }
    }

    @Test
    fun `PaymentException toString 테스트`() {
        // Given
        val exception = PaymentException.InvalidRequest("테스트 메시지")

        // When
        val toString = exception.toString()

        // Then
        assertNotNull(toString)
        assertTrue(toString.contains("InvalidRequest"))
        assertTrue(toString.contains("테스트 메시지"))
    }

    @Test
    fun `PaymentException 스택트레이스 테스트`() {
        // Given
        val exception = PaymentException.PaymentExpired("만료 테스트")

        // When
        val stackTrace = exception.stackTrace

        // Then
        assertNotNull(stackTrace)
        assertTrue(stackTrace.isNotEmpty())
    }
}