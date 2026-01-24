package com.popcorn.payment.dto

import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * DTO 커버리지 향상 테스트
 *
 * [80% 커버리지 달성을 위한 테스트]
 * - Request/Response DTO들의 기본 메서드 테스트
 * - 커버리지 향상을 위한 간단한 테스트들
 */
class DtoCoverageTest {

    @Test
    fun `PaymentConfirmRequest DTO 테스트`() {
        // Given
        val paymentKey = "test_payment_key_123"
        val orderId = UUID.randomUUID().toString()
        val amount = 50000

        // When
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        // Then
        assertEquals(paymentKey, request.paymentKey)
        assertEquals(orderId, request.orderId)
        assertEquals(amount, request.amount)

        assertNotNull(request.toString())
        assertTrue(request.toString().contains("PaymentConfirmRequest"))
        assertTrue(request.toString().contains(paymentKey))
        assertTrue(request.toString().contains(orderId))

        assertEquals(request.hashCode(), request.hashCode()) // 일관성
        assertEquals(request, request) // 반사성

        // copy 테스트
        val copied = request.copy(amount = 75000)
        assertEquals(paymentKey, copied.paymentKey)
        assertEquals(orderId, copied.orderId)
        assertEquals(75000, copied.amount)
    }

    @Test
    fun `PaymentCancelRequest DTO 테스트`() {
        // Given
        val cancelReason = "고객 요청으로 인한 취소"
        val cancelAmount = 25000

        // When
        val request1 = PaymentCancelRequest(cancelReason = cancelReason)
        val request2 = PaymentCancelRequest(
            cancelReason = cancelReason,
            cancelAmount = cancelAmount
        )

        // Then
        assertEquals(cancelReason, request1.cancelReason)
        assertEquals(null, request1.cancelAmount)

        assertEquals(cancelReason, request2.cancelReason)
        assertEquals(cancelAmount, request2.cancelAmount)

        assertNotNull(request1.toString())
        assertNotNull(request2.toString())

        assertTrue(request1.toString().contains(cancelReason))
        assertTrue(request2.toString().contains(cancelReason))
        assertTrue(request2.toString().contains(cancelAmount.toString()))

        // copy 테스트
        val copied = request1.copy(cancelAmount = 30000)
        assertEquals(cancelReason, copied.cancelReason)
        assertEquals(30000, copied.cancelAmount)
    }

    @Test
    fun `PaymentCreateRequest DTO 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = 100000
        val customerId = 12345L
        val orderNo = "ORDER-2024-001"
        val itemName = "테스트 상품"

        // When
        val request1 = PaymentCreateRequest(
            orderId = orderId,
            paymentMethod = paymentMethod,
            amount = amount
        )

        val request2 = PaymentCreateRequest(
            orderId = orderId,
            paymentMethod = paymentMethod,
            amount = amount,
            customerId = customerId,
            orderNo = orderNo,
            itemName = itemName
        )

        // Then
        assertEquals(orderId, request1.orderId)
        assertEquals(paymentMethod, request1.paymentMethod)
        assertEquals(amount, request1.amount)
        assertEquals(null, request1.customerId)
        assertEquals(null, request1.orderNo)
        assertEquals(null, request1.itemName)

        assertEquals(orderId, request2.orderId)
        assertEquals(paymentMethod, request2.paymentMethod)
        assertEquals(amount, request2.amount)
        assertEquals(customerId, request2.customerId)
        assertEquals(orderNo, request2.orderNo)
        assertEquals(itemName, request2.itemName)

        assertNotNull(request1.toString())
        assertNotNull(request2.toString())

        assertTrue(request1.toString().contains(orderId.toString()))
        assertTrue(request1.toString().contains(paymentMethod))
        assertTrue(request2.toString().contains(orderNo))
        assertTrue(request2.toString().contains(itemName))
    }

    @Test
    fun `PaymentStatusUpdateRequest DTO 테스트`() {
        // Given
        val status = "PAID"
        val reason = "결제 승인 완료"

        // When
        val request1 = PaymentStatusUpdateRequest(status = status)
        val request2 = PaymentStatusUpdateRequest(
            status = status,
            reason = reason
        )

        // Then
        assertEquals(status, request1.status)
        assertEquals(null, request1.reason)

        assertEquals(status, request2.status)
        assertEquals(reason, request2.reason)

        assertNotNull(request1.toString())
        assertNotNull(request2.toString())

        assertTrue(request1.toString().contains(status))
        assertTrue(request2.toString().contains(status))
        assertTrue(request2.toString().contains(reason))

        // copy 테스트
        val copied = request1.copy(reason = "새로운 사유")
        assertEquals(status, copied.status)
        assertEquals("새로운 사유", copied.reason)
    }

    @Test
    fun `DTO들의 extremeValues 테스트`() {
        // Given - 극한 값들
        val extremeValues = listOf(
            Triple(1, "A", 1L),
            Triple(Int.MAX_VALUE, "Z".repeat(200), Long.MAX_VALUE),
            Triple(1000, "한글테스트", 999999L),
            Triple(0, "", 0L)
        )

        extremeValues.forEach { (amount, text, longValue) ->
            // PaymentConfirmRequest
            val confirmRequest = PaymentConfirmRequest(
                paymentKey = text.ifEmpty { "empty" },
                orderId = UUID.randomUUID().toString(),
                amount = amount
            )
            assertNotNull(confirmRequest.toString())
            assertTrue(confirmRequest.hashCode() != 0)

            // PaymentCancelRequest
            val cancelRequest = PaymentCancelRequest(
                cancelReason = text.ifEmpty { "empty reason" },
                cancelAmount = amount
            )
            assertNotNull(cancelRequest.toString())

            // PaymentCreateRequest
            val createRequest = PaymentCreateRequest(
                orderId = UUID.randomUUID(),
                paymentMethod = "CARD",
                amount = amount,
                customerId = longValue,
                orderNo = text.ifEmpty { "ORDER-EMPTY" },
                itemName = text.ifEmpty { "Empty Item" }
            )
            assertNotNull(createRequest.toString())

            // PaymentStatusUpdateRequest
            val statusRequest = PaymentStatusUpdateRequest(
                status = "PAID",
                reason = text.ifEmpty { "Empty reason" }
            )
            assertNotNull(statusRequest.toString())
        }
    }

    @Test
    fun `DTO component functions 테스트`() {
        // Given
        val confirmRequest = PaymentConfirmRequest(
            paymentKey = "test-key",
            orderId = "test-order-id",
            amount = 50000
        )

        // When - destructuring
        val (paymentKey, orderId, amount) = confirmRequest

        // Then
        assertEquals("test-key", paymentKey)
        assertEquals("test-order-id", orderId)
        assertEquals(50000, amount)

        // PaymentCreateRequest destructuring
        val createRequest = PaymentCreateRequest(
            orderId = UUID.randomUUID(),
            paymentMethod = "TRANSFER",
            amount = 75000,
            customerId = 12345L,
            orderNo = "ORDER-123",
            itemName = "Test Item"
        )

        val (orderIdComponent, methodComponent, amountComponent, customerIdComponent, orderNoComponent, itemNameComponent) = createRequest

        assertEquals(createRequest.orderId, orderIdComponent)
        assertEquals("TRANSFER", methodComponent)
        assertEquals(75000, amountComponent)
        assertEquals(12345L, customerIdComponent)
        assertEquals("ORDER-123", orderNoComponent)
        assertEquals("Test Item", itemNameComponent)
    }
}