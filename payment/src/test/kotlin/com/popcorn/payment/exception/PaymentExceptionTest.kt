package com.popcorn.payment.exception

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PaymentException 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 모든 예외 클래스의 생성과 메시지 확인
 * - Sealed class와 companion object 테스트
 * - 예외 계층 구조 검증
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentExceptionTest {

    @Test
    fun `PaymentNotFound 예외 생성 테스트`() {
        // Given & When
        val exception1 = PaymentException.PaymentNotFound()
        val exception2 = PaymentException.PaymentNotFound.create()
        val exception3 = PaymentException.paymentNotFound()

        // Then
        assertEquals("결제 정보를 찾을 수 없습니다.", exception1.message)
        assertEquals("결제 정보를 찾을 수 없습니다.", exception2.message)
        assertEquals("결제 정보를 찾을 수 없습니다.", exception3.message)
        assertTrue(exception1 is PaymentException)
        assertTrue(exception1 is RuntimeException)
    }

    @Test
    fun `PaymentNotFound 커스텀 메시지 테스트`() {
        // Given
        val customMessage = "특정 결제 ID를 찾을 수 없습니다."

        // When
        val exception = PaymentException.PaymentNotFound(customMessage)

        // Then
        assertEquals(customMessage, exception.message)
    }

    @Test
    fun `InvalidRequest 예외 생성 테스트`() {
        // Given & When
        val exception1 = PaymentException.InvalidRequest()
        val exception2 = PaymentException.InvalidRequest.create()
        val exception3 = PaymentException.InvalidRequest.create("커스텀 메시지")
        val exception4 = PaymentException.invalidRequest()
        val exception5 = PaymentException.invalidRequest("다른 메시지")

        // Then
        assertEquals("잘못된 요청입니다.", exception1.message)
        assertEquals("잘못된 요청입니다.", exception2.message)
        assertEquals("커스텀 메시지", exception3.message)
        assertEquals("잘못된 요청입니다.", exception4.message)
        assertEquals("다른 메시지", exception5.message)
    }

    @Test
    fun `DuplicatePaymentAttempt 예외 생성 테스트`() {
        // Given & When
        val exception1 = PaymentException.DuplicatePaymentAttempt()
        val exception2 = PaymentException.DuplicatePaymentAttempt.create()
        val exception3 = PaymentException.duplicatePaymentAttempt()

        // Then
        assertEquals("이미 처리 중인 결제가 있습니다.", exception1.message)
        assertEquals("이미 처리 중인 결제가 있습니다.", exception2.message)
        assertEquals("이미 처리 중인 결제가 있습니다.", exception3.message)
    }

    @Test
    fun `ExternalApiError 예외 생성 테스트`() {
        // Given
        val message = "외부 API 호출 실패"
        val cause = RuntimeException("원인 예외")

        // When
        val exception1 = PaymentException.ExternalApiError(message)
        val exception2 = PaymentException.ExternalApiError(message, cause)
        val exception3 = PaymentException.ExternalApiError.create(message)
        val exception4 = PaymentException.ExternalApiError.create(message, cause)
        val exception5 = PaymentException.externalApiError(message)
        val exception6 = PaymentException.externalApiError(message, cause)

        // Then
        assertEquals(message, exception1.message)
        assertEquals(message, exception2.message)
        assertEquals(cause, exception2.cause)
        assertEquals(message, exception3.message)
        assertEquals(message, exception4.message)
        assertEquals(cause, exception4.cause)
        assertEquals(message, exception5.message)
        assertEquals(message, exception6.message)
        assertEquals(cause, exception6.cause)
    }

    @Test
    fun `InvalidStatusTransition 예외 생성 테스트`() {
        // Given & When
        val exception1 = PaymentException.InvalidStatusTransition()
        val exception2 = PaymentException.InvalidStatusTransition.create()
        val exception3 = PaymentException.InvalidStatusTransition.create("커스텀 상태 전환 오류")
        val exception4 = PaymentException.invalidStatusTransition()
        val exception5 = PaymentException.invalidStatusTransition("다른 상태 전환 오류")

        // Then
        assertEquals("유효하지 않은 결제 상태 전환입니다.", exception1.message)
        assertEquals("유효하지 않은 결제 상태 전환입니다.", exception2.message)
        assertEquals("커스텀 상태 전환 오류", exception3.message)
        assertEquals("유효하지 않은 결제 상태 전환입니다.", exception4.message)
        assertEquals("다른 상태 전환 오류", exception5.message)
    }

    @Test
    fun `AmountMismatch 예외 생성 테스트`() {
        // Given & When
        val exception1 = PaymentException.AmountMismatch()
        val exception2 = PaymentException.AmountMismatch.create()
        val exception3 = PaymentException.AmountMismatch.create("금액이 다릅니다")
        val exception4 = PaymentException.amountMismatch()
        val exception5 = PaymentException.amountMismatch("금액 불일치 오류")

        // Then
        assertEquals("결제 금액이 일치하지 않습니다.", exception1.message)
        assertEquals("결제 금액이 일치하지 않습니다.", exception2.message)
        assertEquals("금액이 다릅니다", exception3.message)
        assertEquals("결제 금액이 일치하지 않습니다.", exception4.message)
        assertEquals("금액 불일치 오류", exception5.message)
    }

    @Test
    fun `PaymentExpired 예외 생성 테스트`() {
        // Given & When
        val exception1 = PaymentException.PaymentExpired()
        val exception2 = PaymentException.PaymentExpired.create()
        val exception3 = PaymentException.paymentExpired()

        // Then
        assertEquals("결제 가능 시간이 초과되었습니다.", exception1.message)
        assertEquals("결제 가능 시간이 초과되었습니다.", exception2.message)
        assertEquals("결제 가능 시간이 초과되었습니다.", exception3.message)
    }

    @Test
    fun `PaymentExpired 커스텀 메시지 테스트`() {
        // Given
        val customMessage = "30분 시간 제한이 초과되었습니다."

        // When
        val exception = PaymentException.PaymentExpired(customMessage)

        // Then
        assertEquals(customMessage, exception.message)
    }

    @Test
    fun `예외 계층 구조 테스트`() {
        // Given
        val paymentNotFound = PaymentException.PaymentNotFound()
        val invalidRequest = PaymentException.InvalidRequest()
        val duplicateAttempt = PaymentException.DuplicatePaymentAttempt()
        val externalApiError = PaymentException.ExternalApiError("테스트")
        val invalidTransition = PaymentException.InvalidStatusTransition()
        val amountMismatch = PaymentException.AmountMismatch()
        val paymentExpired = PaymentException.PaymentExpired()

        // Then - 모든 예외가 PaymentException의 서브타입인지 확인
        assertTrue(paymentNotFound is PaymentException)
        assertTrue(invalidRequest is PaymentException)
        assertTrue(duplicateAttempt is PaymentException)
        assertTrue(externalApiError is PaymentException)
        assertTrue(invalidTransition is PaymentException)
        assertTrue(amountMismatch is PaymentException)
        assertTrue(paymentExpired is PaymentException)

        // Then - 모든 예외가 RuntimeException의 서브타입인지 확인
        assertTrue(paymentNotFound is RuntimeException)
        assertTrue(invalidRequest is RuntimeException)
        assertTrue(duplicateAttempt is RuntimeException)
        assertTrue(externalApiError is RuntimeException)
        assertTrue(invalidTransition is RuntimeException)
        assertTrue(amountMismatch is RuntimeException)
        assertTrue(paymentExpired is RuntimeException)
    }

    @Test
    fun `cause가 있는 예외 생성 테스트`() {
        // Given
        val originalException = IllegalArgumentException("원본 예외")

        // When
        val paymentException = PaymentException.ExternalApiError("외부 API 오류", originalException)

        // Then
        assertEquals("외부 API 오류", paymentException.message)
        assertEquals(originalException, paymentException.cause)
        assertNotNull(paymentException.stackTrace)
    }

    @Test
    fun `예외 메시지 null 처리 테스트`() {
        // Given
        val message = "테스트 메시지"

        // When
        val exception = PaymentException.InvalidRequest(message)

        // Then
        assertEquals(message, exception.message)
        assertNotNull(exception.toString())
        assertTrue(exception.toString().contains(message))
    }
}