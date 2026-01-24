package com.popcorn.payment.service

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaymentResultClassesTest {

    @Test
    fun `PaymentCreateResult 생성 테스트`() {
        // Given
        val orderId = "ORDER-123"
        val amount = 10000
        val paymentUrl = "https://test.payment.url"
        val expiresAt = LocalDateTime.now().plusMinutes(15)

        // When
        val result = PaymentCreateResult(
            paymentUrl = paymentUrl,
            orderId = orderId,
            amount = amount,
            expiresAt = expiresAt
        )

        // Then
        assertEquals(orderId, result.orderId)
        assertEquals(amount, result.amount)
        assertEquals(paymentUrl, result.paymentUrl)
        assertEquals(expiresAt, result.expiresAt)
    }

    @Test
    fun `PaymentCreateResult copy 메서드 테스트`() {
        // Given
        val original = PaymentCreateResult(
            paymentUrl = "https://original.url",
            orderId = "ORDER-123",
            amount = 10000,
            expiresAt = LocalDateTime.now()
        )

        // When
        val copied = original.copy(
            amount = 20000,
            paymentUrl = "https://new.url"
        )

        // Then
        assertEquals(original.orderId, copied.orderId)
        assertEquals(20000, copied.amount)
        assertEquals("https://new.url", copied.paymentUrl)
        assertEquals(original.expiresAt, copied.expiresAt)
    }

    @Test
    fun `PaymentCreationResult 생성 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val status = "READY"
        val amount = 25000
        val createdAt = LocalDateTime.now()

        // When
        val result = PaymentCreationResult(
            paymentId = paymentId,
            status = status,
            amount = amount,
            createdAt = createdAt
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(status, result.status)
        assertEquals(amount, result.amount)
        assertEquals(createdAt, result.createdAt)
    }

    @Test
    fun `PaymentDetailResult 생성 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val status = "PAID"
        val amount = 35000
        val approvedAt = LocalDateTime.now()
        val rawPayload = """{"result":"success"}"""

        // When
        val result = PaymentDetailResult(
            paymentId = paymentId,
            orderId = orderId,
            status = status,
            amount = amount,
            approvedAt = approvedAt,
            rawPayload = rawPayload
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(status, result.status)
        assertEquals(amount, result.amount)
        assertEquals(approvedAt, result.approvedAt)
        assertEquals(rawPayload, result.rawPayload)
    }

    @Test
    fun `PaymentDetailResult rawPayload null 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val status = "READY"
        val amount = 45000
        val approvedAt = LocalDateTime.now()

        // When
        val result = PaymentDetailResult(
            paymentId = paymentId,
            orderId = orderId,
            status = status,
            amount = amount,
            approvedAt = approvedAt,
            rawPayload = null
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(status, result.status)
        assertEquals(amount, result.amount)
        assertEquals(approvedAt, result.approvedAt)
        assertEquals(null, result.rawPayload)
    }

    @Test
    fun `TossPaymentConfirmResult 생성 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val paymentStatus = "DONE"
        val orderStatus = "COMPLETED"
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-123"
        val amount = 55000
        val approvedAt = LocalDateTime.now()

        // When
        val result = TossPaymentConfirmResult(
            paymentId = paymentId,
            paymentStatus = paymentStatus,
            orderStatus = orderStatus,
            orderId = orderId,
            orderNo = orderNo,
            amount = amount,
            approvedAt = approvedAt
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(paymentStatus, result.paymentStatus)
        assertEquals(orderStatus, result.orderStatus)
        assertEquals(orderId, result.orderId)
        assertEquals(orderNo, result.orderNo)
        assertEquals(amount, result.amount)
        assertEquals(approvedAt, result.approvedAt)
    }

    @Test
    fun `TossPaymentCancelResult 생성 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val cancelAmount = 65000
        val status = "CANCELLED"
        val cancelReason = "고객 요청"

        // When
        val result = TossPaymentCancelResult(
            paymentId = paymentId,
            orderId = orderId,
            cancelAmount = cancelAmount,
            status = status,
            cancelReason = cancelReason
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(cancelAmount, result.cancelAmount)
        assertEquals(status, result.status)
        assertEquals(cancelReason, result.cancelReason)
    }

    @Test
    fun `PaymentTokenPayload 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 75000
        val issuedAtMillis = System.currentTimeMillis()

        // When
        val payload = PaymentTokenPayload(
            orderId = orderId,
            amount = amount,
            issuedAtMillis = issuedAtMillis
        )

        // Then
        assertEquals(orderId, payload.orderId)
        assertEquals(amount, payload.amount)
        assertEquals(issuedAtMillis, payload.issuedAtMillis)
    }

    @Test
    fun `다양한 상태의 PaymentCreationResult 테스트`() {
        // Given
        val statuses = listOf("READY", "IN_PROGRESS", "WAITING_FOR_DEPOSIT", "DONE", "CANCELED", "PARTIAL_CANCELED", "ABORTED", "EXPIRED")

        statuses.forEach { status ->
            // When
            val result = PaymentCreationResult(
                paymentId = UUID.randomUUID(),
                status = status,
                amount = 10000,
                createdAt = LocalDateTime.now()
            )

            // Then
            assertEquals(status, result.status)
            assertNotNull(result.paymentId)
            assertNotNull(result.createdAt)
        }
    }

    @Test
    fun `다양한 금액의 PaymentDetailResult 테스트`() {
        // Given
        val amounts = listOf(1000, 5000, 10000, 50000, 100000, 500000, 1000000)

        amounts.forEach { amount ->
            // When
            val result = PaymentDetailResult(
                paymentId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                status = "PAID",
                amount = amount,
                approvedAt = LocalDateTime.now(),
                rawPayload = null
            )

            // Then
            assertEquals(amount, result.amount)
            assertEquals("PAID", result.status)
        }
    }
}