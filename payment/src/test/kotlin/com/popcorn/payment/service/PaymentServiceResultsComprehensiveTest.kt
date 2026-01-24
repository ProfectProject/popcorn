package com.popcorn.payment.service

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 결제 서비스 Result 클래스들의 포괄적 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 모든 데이터 클래스의 완전한 메서드 테스트
 * - equals, hashCode, toString, copy 메서드 커버
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentServiceResultsComprehensiveTest {

    @Test
    fun `PaymentCreationResult 전체 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val status = "READY"
        val amount = 10000
        val createdAt = LocalDateTime.now()

        // When
        val result = PaymentCreationResult(paymentId, status, amount, createdAt)

        // Then - 기본 프로퍼티
        assertEquals(paymentId, result.paymentId)
        assertEquals(status, result.status)
        assertEquals(amount, result.amount)
        assertEquals(createdAt, result.createdAt)

        // Then - toString
        val toString = result.toString()
        assertNotNull(toString)
        assertTrue(toString.contains(paymentId.toString()))
        assertTrue(toString.contains(status))
        assertTrue(toString.contains(amount.toString()))

        // Then - equals와 hashCode
        val same = PaymentCreationResult(paymentId, status, amount, createdAt)
        val different = PaymentCreationResult(UUID.randomUUID(), status, amount, createdAt)

        assertEquals(result, same)
        assertEquals(result.hashCode(), same.hashCode())
        assertFalse(result == different)
        assertTrue(result.hashCode() != different.hashCode())

        // Then - copy (data class)
        val copied = result.copy(status = "PAID")
        assertEquals(paymentId, copied.paymentId)
        assertEquals("PAID", copied.status)
        assertEquals(amount, copied.amount)
        assertEquals(createdAt, copied.createdAt)
    }

    @Test
    fun `PaymentDetailResult 전체 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentKey = "test_key"
        val status = "PAID"
        val amount = 20000
        val approvedAt = LocalDateTime.now()
        val rawPayload = """{"test": "data"}"""

        // When
        val result = PaymentDetailResult(
            paymentId = paymentId,
            orderId = orderId,
            paymentKey = paymentKey,
            status = status,
            amount = amount,
            approvedAt = approvedAt,
            rawPayload = rawPayload
        )

        // Then - 기본 프로퍼티
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(paymentKey, result.paymentKey)
        assertEquals(status, result.status)
        assertEquals(amount, result.amount)
        assertEquals(approvedAt, result.approvedAt)
        assertEquals(rawPayload, result.rawPayload)

        // Then - toString
        val toString = result.toString()
        assertNotNull(toString)
        assertTrue(toString.contains(paymentId.toString()))
        assertTrue(toString.contains(status))

        // Then - equals와 hashCode
        val same = PaymentDetailResult(
            paymentId, orderId, paymentKey, status, amount, approvedAt, rawPayload
        )
        val different = PaymentDetailResult(
            UUID.randomUUID(), orderId, paymentKey, status, amount, approvedAt, rawPayload
        )

        assertEquals(result, same)
        assertEquals(result.hashCode(), same.hashCode())
        assertFalse(result == different)

        // Then - copy
        val copied = result.copy(status = "CANCELLED")
        assertEquals(paymentId, copied.paymentId)
        assertEquals("CANCELLED", copied.status)
        assertEquals(amount, copied.amount)
    }

    @Test
    fun `PaymentDetailResult null 값 처리 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()

        // When - null 값들로 생성
        val result = PaymentDetailResult(
            paymentId = paymentId,
            orderId = null,
            paymentKey = null,
            status = "READY",
            amount = 5000,
            approvedAt = null,
            rawPayload = null
        )

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(null, result.orderId)
        assertEquals(null, result.paymentKey)
        assertEquals("READY", result.status)
        assertEquals(5000, result.amount)
        assertEquals(null, result.approvedAt)
        assertEquals(null, result.rawPayload)

        // toString도 null 값들과 잘 동작하는지 확인
        val toString = result.toString()
        assertNotNull(toString)
        assertTrue(toString.contains(paymentId.toString()))
    }

    @Test
    fun `TossPaymentConfirmResult 전체 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val paymentStatus = "PAID"
        val orderStatus = "COMPLETED"
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-123"
        val amount = 30000
        val approvedAt = LocalDateTime.now()

        // When
        val result = TossPaymentConfirmResult(
            paymentId, paymentStatus, orderStatus, orderId, orderNo, amount, approvedAt
        )

        // Then - 기본 프로퍼티
        assertEquals(paymentId, result.paymentId)
        assertEquals(paymentStatus, result.paymentStatus)
        assertEquals(orderStatus, result.orderStatus)
        assertEquals(orderId, result.orderId)
        assertEquals(orderNo, result.orderNo)
        assertEquals(amount, result.amount)
        assertEquals(approvedAt, result.approvedAt)

        // Then - toString
        val toString = result.toString()
        assertNotNull(toString)
        assertTrue(toString.contains(paymentStatus))
        assertTrue(toString.contains(orderNo))

        // Then - equals와 hashCode
        val same = TossPaymentConfirmResult(
            paymentId, paymentStatus, orderStatus, orderId, orderNo, amount, approvedAt
        )
        val different = TossPaymentConfirmResult(
            paymentId, "CANCELLED", orderStatus, orderId, orderNo, amount, approvedAt
        )

        assertEquals(result, same)
        assertEquals(result.hashCode(), same.hashCode())
        assertFalse(result == different)

        // Then - copy
        val copied = result.copy(paymentStatus = "CANCELLED", orderStatus = "CANCELLED")
        assertEquals(paymentId, copied.paymentId)
        assertEquals("CANCELLED", copied.paymentStatus)
        assertEquals("CANCELLED", copied.orderStatus)
    }

    @Test
    fun `TossPaymentCancelResult 전체 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val cancelAmount = 15000
        val status = "CANCELLED"
        val cancelReason = "고객 요청"

        // When
        val result = TossPaymentCancelResult(paymentId, orderId, cancelAmount, status, cancelReason)

        // Then - 기본 프로퍼티
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(cancelAmount, result.cancelAmount)
        assertEquals(status, result.status)
        assertEquals(cancelReason, result.cancelReason)

        // Then - toString
        val toString = result.toString()
        assertNotNull(toString)
        assertTrue(toString.contains(status))
        assertTrue(toString.contains(cancelReason))

        // Then - equals와 hashCode
        val same = TossPaymentCancelResult(paymentId, orderId, cancelAmount, status, cancelReason)
        val different = TossPaymentCancelResult(paymentId, orderId, 20000, status, cancelReason)

        assertEquals(result, same)
        assertEquals(result.hashCode(), same.hashCode())
        assertFalse(result == different)

        // Then - copy
        val copied = result.copy(cancelAmount = 20000)
        assertEquals(paymentId, copied.paymentId)
        assertEquals(20000, copied.cancelAmount)
    }

    @Test
    fun `PaymentCreateResult 전체 테스트`() {
        // Given
        val paymentUrl = "https://payment.test.com"
        val orderId = "ORDER-456"
        val amount = 40000
        val expiresAt = LocalDateTime.now().plusMinutes(30)

        // When
        val result = PaymentCreateResult(paymentUrl, orderId, amount, expiresAt)

        // Then - 기본 프로퍼티
        assertEquals(paymentUrl, result.paymentUrl)
        assertEquals(orderId, result.orderId)
        assertEquals(amount, result.amount)
        assertEquals(expiresAt, result.expiresAt)

        // Then - toString
        val toString = result.toString()
        assertNotNull(toString)
        assertTrue(toString.contains(paymentUrl))
        assertTrue(toString.contains(orderId))

        // Then - equals와 hashCode
        val same = PaymentCreateResult(paymentUrl, orderId, amount, expiresAt)
        val different = PaymentCreateResult("https://different.url", orderId, amount, expiresAt)

        assertEquals(result, same)
        assertEquals(result.hashCode(), same.hashCode())
        assertFalse(result == different)

        // Then - copy
        val copied = result.copy(amount = 50000)
        assertEquals(paymentUrl, copied.paymentUrl)
        assertEquals(50000, copied.amount)
    }

    @Test
    fun `QrIssuanceTracker 실제 동작 테스트`() {
        // Given
        val tracker = QrIssuanceTracker()
        val orderId1 = UUID.randomUUID()
        val orderId2 = UUID.randomUUID()

        // When & Then - 새로운 주문 시작 테스트
        assertTrue(tracker.tryStart(orderId1))
        assertTrue(tracker.tryStart(orderId2))

        // When & Then - 중복 시작 불가 테스트
        assertFalse(tracker.tryStart(orderId1))
        assertFalse(tracker.tryStart(orderId2))

        // When & Then - 성공 처리 후 다시 시작 불가
        tracker.markSuccess(orderId1)
        assertFalse(tracker.tryStart(orderId1))

        // When & Then - 실패 처리 후 재시작 가능
        tracker.markFailure(orderId2)
        assertTrue(tracker.tryStart(orderId2))
    }

    @Test
    fun `다양한 조건의 결과 객체들 테스트`() {
        // 극한값들로 테스트
        val extremeValues = listOf(
            Triple(1, LocalDateTime.MIN, "MIN"),
            Triple(999999999, LocalDateTime.MAX, "MAX"),
            Triple(0, LocalDateTime.now(), "ZERO")
        )

        extremeValues.forEach { (amount, dateTime, label) ->
            val paymentId = UUID.randomUUID()

            // PaymentCreationResult
            val creationResult = PaymentCreationResult(paymentId, "READY", amount, dateTime)
            assertEquals(amount, creationResult.amount)
            assertNotNull(creationResult.toString())

            // PaymentDetailResult
            val detailResult = PaymentDetailResult(
                paymentId, UUID.randomUUID(), "key_$label", "READY", amount, dateTime, "{}"
            )
            assertEquals(amount, detailResult.amount)
            assertNotNull(detailResult.toString())
        }
    }

    @Test
    fun `복사본 독립성 테스트`() {
        // Given
        val original = PaymentDetailResult(
            UUID.randomUUID(), UUID.randomUUID(), "original", "READY", 10000, LocalDateTime.now(), "{}"
        )

        // When
        val copy1 = original.copy(status = "PAID")
        val copy2 = original.copy(amount = 20000)

        // Then - 각각 독립적으로 변경됨
        assertEquals("READY", original.status)
        assertEquals("PAID", copy1.status)
        assertEquals("READY", copy2.status)

        assertEquals(10000, original.amount)
        assertEquals(10000, copy1.amount)
        assertEquals(20000, copy2.amount)

        // 서로 다른 객체임을 확인
        assertFalse(original === copy1)
        assertFalse(original === copy2)
        assertFalse(copy1 === copy2)
    }
}