package com.popcorn.payment.dto

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PaymentResponseDto 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 모든 DTO 클래스의 생성과 프로퍼티 접근 테스트
 * - Data class의 copy, equals, hashCode, toString 메소드 테스트
 * - Companion object factory 메소드 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentResponseDtoTest {

    @Test
    fun `PaymentConfirmResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-12345"
        val amount = 15000
        val approvedAt = LocalDateTime.now()

        // When
        val response = PaymentConfirmResponse(
            paymentId = paymentId,
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = orderId,
            orderNo = orderNo,
            amount = amount,
            approvedAt = approvedAt
        )

        // Then
        assertEquals(paymentId, response.paymentId)
        assertEquals("PAID", response.paymentStatus)
        assertEquals("COMPLETED", response.orderStatus)
        assertEquals(orderId, response.orderId)
        assertEquals(orderNo, response.orderNo)
        assertEquals(amount, response.amount)
        assertEquals(approvedAt, response.approvedAt)
    }

    @Test
    fun `PaymentCancelResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val cancelAmount = 10000
        val cancelReason = "사용자 요청"

        // When
        val response = PaymentCancelResponse(
            paymentId = paymentId,
            orderId = orderId,
            cancelAmount = cancelAmount,
            status = "CANCELLED",
            cancelReason = cancelReason
        )

        // Then
        assertEquals(paymentId, response.paymentId)
        assertEquals(orderId, response.orderId)
        assertEquals(cancelAmount, response.cancelAmount)
        assertEquals("CANCELLED", response.status)
        assertEquals(cancelReason, response.cancelReason)
    }

    @Test
    fun `PaymentCreateResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val amount = 25000
        val createdAt = LocalDateTime.now()
        val expiresAt = LocalDateTime.now().plusMinutes(30)
        val paymentUrl = "https://checkout.tosspayments.com/v1/payment"

        // When
        val response = PaymentCreateResponse(
            paymentId = paymentId,
            orderId = orderId,
            amount = amount,
            status = "READY",
            paymentMethod = "CARD",
            createdAt = createdAt,
            paymentUrl = paymentUrl,
            expiresAt = expiresAt
        )

        // Then
        assertEquals(paymentId, response.paymentId)
        assertEquals(orderId, response.orderId)
        assertEquals(amount, response.amount)
        assertEquals("READY", response.status)
        assertEquals("CARD", response.paymentMethod)
        assertEquals(createdAt, response.createdAt)
        assertEquals(paymentUrl, response.paymentUrl)
        assertEquals(expiresAt, response.expiresAt)
    }

    @Test
    fun `PaymentCreateResponse 기본값 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val createdAt = LocalDateTime.now()

        // When
        val response = PaymentCreateResponse(
            paymentId = null,
            orderId = orderId,
            amount = 5000,
            status = "PENDING",
            paymentMethod = "TRANSFER",
            createdAt = createdAt
            // paymentUrl, expiresAt은 기본값 null 사용
        )

        // Then
        assertNull(response.paymentId)
        assertNull(response.paymentUrl)
        assertNull(response.expiresAt)
    }

    @Test
    fun `PaymentTokenDecodeResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-54321"
        val amount = 30000
        val customerKey = "customer_abc123"
        val successUrl = "https://example.com/success"
        val failUrl = "https://example.com/fail"

        // When
        val response = PaymentTokenDecodeResponse(
            orderId = orderId,
            orderNo = orderNo,
            amount = amount,
            customerKey = customerKey,
            successUrl = successUrl,
            failUrl = failUrl
        )

        // Then
        assertEquals(orderId, response.orderId)
        assertEquals(orderNo, response.orderNo)
        assertEquals(amount, response.amount)
        assertEquals(customerKey, response.customerKey)
        assertEquals(successUrl, response.successUrl)
        assertEquals(failUrl, response.failUrl)
    }

    @Test
    fun `PaymentDetailResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val amount = 12000
        val createdAt = LocalDateTime.now()
        val approvedAt = LocalDateTime.now().plusMinutes(5)
        val updatedAt = LocalDateTime.now().plusMinutes(10)

        // When
        val response = PaymentDetailResponse(
            paymentId = paymentId,
            orderId = orderId,
            amount = amount,
            status = "COMPLETED",
            paymentMethod = "VIRTUAL_ACCOUNT",
            createdAt = createdAt,
            approvedAt = approvedAt,
            updatedAt = updatedAt
        )

        // Then
        assertEquals(paymentId, response.paymentId)
        assertEquals(orderId, response.orderId)
        assertEquals(amount, response.amount)
        assertEquals("COMPLETED", response.status)
        assertEquals("VIRTUAL_ACCOUNT", response.paymentMethod)
        assertEquals(createdAt, response.createdAt)
        assertEquals(approvedAt, response.approvedAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `PaymentStatusResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val approvedAt = LocalDateTime.now()

        // When
        val response = PaymentStatusResponse(
            paymentId = paymentId,
            orderId = orderId,
            status = "APPROVED",
            approvedAt = approvedAt
        )

        // Then
        assertEquals(paymentId, response.paymentId)
        assertEquals(orderId, response.orderId)
        assertEquals("APPROVED", response.status)
        assertEquals(approvedAt, response.approvedAt)
    }

    @Test
    fun `PaymentListResponse 생성 및 프로퍼티 테스트`() {
        // Given
        val payment1 = PaymentDetailResponse(
            paymentId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            amount = 10000,
            status = "PAID",
            paymentMethod = "CARD",
            createdAt = LocalDateTime.now(),
            approvedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val payment2 = PaymentDetailResponse(
            paymentId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            amount = 15000,
            status = "PAID",
            paymentMethod = "TRANSFER",
            createdAt = LocalDateTime.now(),
            approvedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val payments = listOf(payment1, payment2)

        // When
        val response = PaymentListResponse(
            payments = payments,
            totalCount = 2,
            totalAmount = 25000L
        )

        // Then
        assertEquals(payments, response.payments)
        assertEquals(2, response.totalCount)
        assertEquals(25000L, response.totalAmount)
        assertEquals(2, response.payments.size)
    }

    @Test
    fun `ApiResponse success 팩토리 메소드 테스트`() {
        // Given
        val data = "테스트 데이터"
        val message = "성공적으로 처리되었습니다."

        // When
        val response1 = ApiResponse.success(data)
        val response2 = ApiResponse.success(data, message)

        // Then
        assertTrue(response1.success)
        assertEquals(data, response1.data)
        assertEquals("요청이 성공적으로 처리되었습니다.", response1.message)
        assertNull(response1.errorCode)

        assertTrue(response2.success)
        assertEquals(data, response2.data)
        assertEquals(message, response2.message)
        assertNull(response2.errorCode)
    }

    @Test
    fun `ApiResponse error 팩토리 메소드 테스트`() {
        // Given
        val errorMessage = "오류가 발생했습니다."
        val errorCode = "PAYMENT_ERROR"

        // When
        val response1 = ApiResponse.error<String>(errorMessage)
        val response2 = ApiResponse.error<String>(errorMessage, errorCode)

        // Then
        assertFalse(response1.success)
        assertNull(response1.data)
        assertEquals(errorMessage, response1.message)
        assertNull(response1.errorCode)

        assertFalse(response2.success)
        assertNull(response2.data)
        assertEquals(errorMessage, response2.message)
        assertEquals(errorCode, response2.errorCode)
    }

    @Test
    fun `ApiResponse 다양한 데이터 타입 테스트`() {
        // Given & When
        val stringResponse = ApiResponse.success("문자열 데이터")
        val numberResponse = ApiResponse.success(42)
        val booleanResponse = ApiResponse.success(true)
        val nullResponse = ApiResponse.success<String?>(null)

        // Then
        assertEquals("문자열 데이터", stringResponse.data)
        assertEquals(42, numberResponse.data)
        assertEquals(true, booleanResponse.data)
        assertNull(nullResponse.data)
    }

    @Test
    fun `Data class copy 메소드 테스트`() {
        // Given
        val original = PaymentConfirmResponse(
            paymentId = UUID.randomUUID(),
            paymentStatus = "PENDING",
            orderStatus = "PENDING",
            orderId = UUID.randomUUID(),
            orderNo = "ORDER-123",
            amount = 10000,
            approvedAt = null
        )

        // When
        val copied = original.copy(
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            approvedAt = LocalDateTime.now()
        )

        // Then
        assertEquals(original.paymentId, copied.paymentId)
        assertEquals(original.orderId, copied.orderId)
        assertEquals(original.orderNo, copied.orderNo)
        assertEquals(original.amount, copied.amount)
        assertEquals("PAID", copied.paymentStatus)
        assertEquals("COMPLETED", copied.orderStatus)
        assertTrue(copied.approvedAt != null)
    }

    @Test
    fun `Data class equals 및 hashCode 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val response1 = PaymentConfirmResponse(
            paymentId = paymentId,
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = orderId,
            orderNo = "ORDER-123",
            amount = 10000,
            approvedAt = null
        )

        val response2 = PaymentConfirmResponse(
            paymentId = paymentId,
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = orderId,
            orderNo = "ORDER-123",
            amount = 10000,
            approvedAt = null
        )

        val response3 = PaymentConfirmResponse(
            paymentId = paymentId,
            paymentStatus = "PENDING",
            orderStatus = "PENDING",
            orderId = orderId,
            orderNo = "ORDER-123",
            amount = 10000,
            approvedAt = null
        )

        // When & Then
        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
        assertTrue(response1 != response3)
        assertTrue(response1.hashCode() != response3.hashCode())
    }
}