package com.popcorn.payment.service

import com.popcorn.payment.entity.PaymentMethod
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 서비스 커버리지 증대를 위한 추가 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 기존 테스트에서 놓친 분기 및 메서드들
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class ServiceCoverageBoostTest {


    @Test
    fun `PaymentDetailResult 빌더 패턴 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val approvedAt = LocalDateTime.now()

        // When
        val result = PaymentDetailResult(
            paymentId = paymentId,
            orderId = orderId,
            paymentKey = "test_payment_key",
            status = "PAID",
            amount = 15000,
            approvedAt = approvedAt,
            rawPayload = """{"method": "card", "amount": 15000}"""
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals("test_payment_key", result.paymentKey)
        assertEquals("PAID", result.status)
        assertEquals(15000, result.amount)
        assertEquals(approvedAt, result.approvedAt)
        assertTrue(result.rawPayload!!.contains("card"))

        // equals 테스트
        val same = result.copy()
        assertEquals(result, same)
        assertEquals(result.hashCode(), same.hashCode())

        // 다른 객체와 비교
        val different = result.copy(amount = 20000)
        assertTrue(result != different)
        assertTrue(result.hashCode() != different.hashCode())
    }

    @Test
    fun `TossPaymentConfirmResult 필드별 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val approvedAt = LocalDateTime.now()

        // When - 다양한 상태별로 테스트
        val statusList = listOf("READY", "PAID", "FAILED", "CANCELLED", "EXPIRED")

        statusList.forEach { status ->
            val result = TossPaymentConfirmResult(
                paymentId = paymentId,
                paymentStatus = status,
                orderStatus = "ORDER_$status",
                orderId = orderId,
                orderNo = "ORDER-$status-123",
                amount = 25000,
                approvedAt = approvedAt // 모든 경우에 approvedAt 제공
            )

            // Then
            assertEquals(paymentId, result.paymentId)
            assertEquals(status, result.paymentStatus)
            assertEquals("ORDER_$status", result.orderStatus)
            assertEquals(orderId, result.orderId)
            assertEquals("ORDER-$status-123", result.orderNo)
            assertEquals(25000, result.amount)
            assertEquals(approvedAt, result.approvedAt)

            assertNotNull(result.toString())
        }
    }

    @Test
    fun `TossPaymentCancelResult 다양한 취소 사유 테스트`() {
        val cancelReasons = listOf(
            "고객 요청",
            "상품 품절",
            "시스템 오류",
            "결제 오류",
            "배송 불가",
            "중복 결제"
        )

        cancelReasons.forEach { reason ->
            // Given
            val paymentId = UUID.randomUUID()
            val orderId = UUID.randomUUID()
            val cancelAmount = (1000..100000).random()

            // When
            val result = TossPaymentCancelResult(
                paymentId = paymentId,
                orderId = orderId,
                cancelAmount = cancelAmount,
                status = "CANCELLED",
                cancelReason = reason
            )

            // Then
            assertEquals(paymentId, result.paymentId)
            assertEquals(orderId, result.orderId)
            assertEquals(cancelAmount, result.cancelAmount)
            assertEquals("CANCELLED", result.status)
            assertEquals(reason, result.cancelReason)
            assertNotNull(result.toString())
        }
    }

    @Test
    fun `PaymentCreateResult URL 형식 테스트`() {
        val testUrls = listOf(
            "https://checkout.tosspayments.com/v1/payment/test123",
            "https://sandbox-pay.toss.im/checkout/test456",
            "https://api.tosspayments.com/v1/payments/test789",
            "https://pay.toss.im/web/checkout/test000"
        )

        testUrls.forEach { url ->
            // Given
            val orderId = "ORDER-${UUID.randomUUID()}"
            val amount = (5000..100000).random()
            val expiresAt = LocalDateTime.now().plusMinutes(30)

            // When
            val result = PaymentCreateResult(
                paymentUrl = url,
                orderId = orderId,
                amount = amount,
                expiresAt = expiresAt
            )

            // Then
            assertEquals(url, result.paymentUrl)
            assertEquals(orderId, result.orderId)
            assertEquals(amount, result.amount)
            assertEquals(expiresAt, result.expiresAt)
            assertNotNull(result.toString())
            assertTrue(result.paymentUrl.startsWith("https://"))
        }
    }

    @Test
    fun `PaymentTokenPayload 극한 테스트`() {
        val extremeTestCases = listOf(
            Triple(UUID.randomUUID(), Int.MIN_VALUE, 0L),
            Triple(UUID.randomUUID(), Int.MAX_VALUE, Long.MAX_VALUE),
            Triple(UUID.randomUUID(), 0, System.currentTimeMillis()),
            Triple(UUID.randomUUID(), 1, 1L),
            Triple(UUID.randomUUID(), -1, -1L)
        )

        extremeTestCases.forEach { (orderId, amount, timestamp) ->
            // When
            val payload = PaymentTokenPayload(
                orderId = orderId,
                amount = amount,
                issuedAtMillis = timestamp
            )

            // Then
            assertEquals(orderId, payload.orderId)
            assertEquals(amount, payload.amount)
            assertEquals(timestamp, payload.issuedAtMillis)
            assertNotNull(payload.toString())

            // copy 테스트
            val copied = payload.copy(amount = amount + 1000)
            assertEquals(amount + 1000, copied.amount)
            assertEquals(orderId, copied.orderId)
            assertEquals(timestamp, copied.issuedAtMillis)
        }
    }

    @Test
    fun `다양한 PaymentMethod와 Amount 조합 테스트`() {
        val methods = PaymentMethod.values()
        val amounts = listOf(1, 100, 1000, 10000, 100000, 999999)

        methods.forEach { method ->
            amounts.forEach { amount ->
                // Given
                val orderId = UUID.randomUUID()
                val paymentKey = "test_${method.name.lowercase()}_$amount"

                // When
                val payload = PaymentTokenPayload(
                    orderId = orderId,
                    amount = amount,
                    issuedAtMillis = System.currentTimeMillis()
                )

                val creationResult = PaymentCreationResult(
                    paymentId = UUID.randomUUID(),
                    status = "READY",
                    amount = amount,
                    createdAt = LocalDateTime.now()
                )

                // Then
                assertEquals(orderId, payload.orderId)
                assertEquals(amount, payload.amount)
                assertEquals(amount, creationResult.amount)
                assertEquals("READY", creationResult.status)
                assertNotNull(payload.toString())
                assertNotNull(creationResult.toString())
            }
        }
    }

}