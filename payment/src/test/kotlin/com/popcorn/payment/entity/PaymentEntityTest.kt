package com.popcorn.payment.entity

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Payment Entity 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - Payment 엔티티의 모든 메서드 및 프로퍼티 테스트
 * - enum 클래스들의 모든 값 및 메서드 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentEntityTest {

    @Test
    fun `Payment 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentKey = "test_payment_key"
        val rawPayload = """{"test": "data"}"""

        // When
        val payment = Payment.create(orderId, PaymentMethod.CARD, 10000, paymentKey, rawPayload)

        // Then
        assertNotNull(payment.id)
        assertEquals(orderId, payment.orderId)
        assertEquals(PaymentMethod.CARD, payment.paymentMethod)
        assertEquals(10000, payment.amount)
        assertEquals(paymentKey, payment.paymentKey)
        assertEquals(PaymentStatus.READY, payment.status)
        assertEquals(rawPayload, payment.rawPayload)
        assertNotNull(payment.createdAt)
    }

    @Test
    fun `Payment updateStatus 메서드 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")
        val approvedAt = LocalDateTime.now()

        // When
        payment.updateStatus(PaymentStatus.PAID, approvedAt)

        // Then
        assertEquals(PaymentStatus.PAID, payment.status)
        assertEquals(approvedAt, payment.approvedAt)
    }

    @Test
    fun `Payment approve 정적 메서드 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")
        val approvedAt = LocalDateTime.now()
        val rawPayload = """{"approved": true}"""

        // When
        val approvedPayment = Payment.approve(payment, approvedAt, rawPayload)

        // Then
        assertEquals(PaymentStatus.PAID, approvedPayment.status)
        assertEquals(approvedAt, approvedPayment.approvedAt)
        assertEquals(rawPayload, approvedPayment.rawPayload)
    }

    @Test
    fun `Payment updateStatus FAILED 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")

        // When
        payment.updateStatus(PaymentStatus.FAILED)

        // Then
        assertEquals(PaymentStatus.FAILED, payment.status)
    }

    @Test
    fun `Payment updateStatus CANCELLED 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")

        // When
        payment.updateStatus(PaymentStatus.CANCELLED)

        // Then
        assertEquals(PaymentStatus.CANCELLED, payment.status)
    }

    @Test
    fun `Payment softDelete 메서드 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")

        // When
        payment.softDelete()

        // Then
        assertTrue(payment.isDeleted)
    }

    @Test
    fun `Payment isNotDeleted 메서드 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")

        // When & Then
        assertTrue(payment.isNotDeleted()) // 처음에는 삭제되지 않음

        // When
        payment.softDelete()

        // Then
        assertFalse(payment.isNotDeleted()) // 삭제 후에는 false
    }

    @Test
    fun `Payment equals 및 hashCode 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payment1 = Payment.create(orderId, PaymentMethod.CARD, 10000, "key1", "{}")
        val payment2 = Payment.create(orderId, PaymentMethod.CARD, 10000, "key2", "{}")

        payment1.id = payment2.id // 동일한 ID 설정

        // When & Then
        assertEquals(payment1, payment2)
        assertEquals(payment1.hashCode(), payment2.hashCode())

        // Given - 다른 ID
        payment2.id = UUID.randomUUID()

        // When & Then
        assertTrue(payment1 != payment2)
        assertTrue(payment1.hashCode() != payment2.hashCode())
    }

    @Test
    fun `Payment toString 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val payment = Payment.create(orderId, PaymentMethod.CARD, 10000, "key", "{}")
        payment.id = paymentId

        // When
        val toString = payment.toString()

        // Then
        assertNotNull(toString)
        assertTrue(toString.contains(paymentId.toString()))
        assertTrue(toString.contains(orderId.toString()))
        assertTrue(toString.contains("10000"))
        assertTrue(toString.contains("READY"))
        assertTrue(toString.contains("CARD"))
    }

    @Test
    fun `PaymentMethod enum 테스트`() {
        // When & Then
        assertEquals("CARD", PaymentMethod.CARD.name)
        assertEquals("TRANSFER", PaymentMethod.TRANSFER.name)
        assertEquals("VIRTUAL_ACCOUNT", PaymentMethod.VIRTUAL_ACCOUNT.name)
        assertEquals("MOBILE_PHONE", PaymentMethod.MOBILE_PHONE.name)
        assertEquals("GIFT_CERTIFICATE", PaymentMethod.GIFT_CERTIFICATE.name)

        // 모든 PaymentMethod 값이 올바르게 정의되었는지 확인
        val allMethods = PaymentMethod.values()
        assertEquals(5, allMethods.size)
        assertTrue(allMethods.contains(PaymentMethod.CARD))
        assertTrue(allMethods.contains(PaymentMethod.TRANSFER))
        assertTrue(allMethods.contains(PaymentMethod.VIRTUAL_ACCOUNT))
        assertTrue(allMethods.contains(PaymentMethod.MOBILE_PHONE))
        assertTrue(allMethods.contains(PaymentMethod.GIFT_CERTIFICATE))
    }

    @Test
    fun `PaymentStatus enum 테스트`() {
        // When & Then
        assertEquals("READY", PaymentStatus.READY.name)
        assertEquals("PAID", PaymentStatus.PAID.name)
        assertEquals("FAILED", PaymentStatus.FAILED.name)
        assertEquals("CANCELLED", PaymentStatus.CANCELLED.name)

        // 모든 PaymentStatus 값이 올바르게 정의되었는지 확인
        val allStatuses = PaymentStatus.values()
        assertEquals(4, allStatuses.size)
        assertTrue(allStatuses.contains(PaymentStatus.READY))
        assertTrue(allStatuses.contains(PaymentStatus.PAID))
        assertTrue(allStatuses.contains(PaymentStatus.FAILED))
        assertTrue(allStatuses.contains(PaymentStatus.CANCELLED))
    }

    @Test
    fun `PaymentMethod valueOf 테스트`() {
        // When & Then
        assertEquals(PaymentMethod.CARD, PaymentMethod.valueOf("CARD"))
        assertEquals(PaymentMethod.TRANSFER, PaymentMethod.valueOf("TRANSFER"))
        assertEquals(PaymentMethod.VIRTUAL_ACCOUNT, PaymentMethod.valueOf("VIRTUAL_ACCOUNT"))
        assertEquals(PaymentMethod.MOBILE_PHONE, PaymentMethod.valueOf("MOBILE_PHONE"))
        assertEquals(PaymentMethod.GIFT_CERTIFICATE, PaymentMethod.valueOf("GIFT_CERTIFICATE"))
    }

    @Test
    fun `PaymentStatus valueOf 테스트`() {
        // When & Then
        assertEquals(PaymentStatus.READY, PaymentStatus.valueOf("READY"))
        assertEquals(PaymentStatus.PAID, PaymentStatus.valueOf("PAID"))
        assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"))
        assertEquals(PaymentStatus.CANCELLED, PaymentStatus.valueOf("CANCELLED"))
    }

    @Test
    fun `Payment 다양한 생성 시나리오 테스트`() {
        val scenarios = listOf(
            Triple(PaymentMethod.CARD, 1, "card_key"),
            Triple(PaymentMethod.TRANSFER, 999999, "transfer_key"),
            Triple(PaymentMethod.VIRTUAL_ACCOUNT, 50000, "va_key"),
            Triple(PaymentMethod.MOBILE_PHONE, 100000, "mobile_key"),
            Triple(PaymentMethod.GIFT_CERTIFICATE, 25000, "gift_key")
        )

        scenarios.forEach { (method, amount, key) ->
            // Given
            val orderId = UUID.randomUUID()
            val rawPayload = """{"method": "$method", "amount": $amount}"""

            // When
            val payment = Payment.create(orderId, method, amount, key, rawPayload)

            // Then
            assertEquals(orderId, payment.orderId)
            assertEquals(method, payment.paymentMethod)
            assertEquals(amount, payment.amount)
            assertEquals(key, payment.paymentKey)
            assertEquals(PaymentStatus.READY, payment.status)
            assertEquals(rawPayload, payment.rawPayload)
        }
    }

    @Test
    fun `Payment 상태 전환 시나리오 테스트`() {
        // Given
        val payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key", "{}")

        // 시나리오 1: READY -> PAID (정적 메서드 사용)
        val approvedAt = LocalDateTime.now()
        val approvedPayment = Payment.approve(payment, approvedAt, """{"approved": true}""")
        assertEquals(PaymentStatus.PAID, approvedPayment.status)
        assertEquals(approvedAt, approvedPayment.approvedAt)

        // 시나리오 2: 새 결제로 READY -> FAILED
        val payment2 = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key2", "{}")
        payment2.updateStatus(PaymentStatus.FAILED)
        assertEquals(PaymentStatus.FAILED, payment2.status)

        // 시나리오 3: 새 결제로 READY -> CANCELLED
        val payment3 = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key3", "{}")
        payment3.updateStatus(PaymentStatus.CANCELLED)
        assertEquals(PaymentStatus.CANCELLED, payment3.status)

        // 시나리오 4: 직접 상태 변경
        val payment4 = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000, "key4", "{}")
        val newApprovedAt = LocalDateTime.now().plusMinutes(1)
        payment4.updateStatus(PaymentStatus.PAID, newApprovedAt)
        assertEquals(PaymentStatus.PAID, payment4.status)
        assertEquals(newApprovedAt, payment4.approvedAt)
    }

    @Test
    fun `Payment null 값 처리 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When - null 값들로 생성
        val payment = Payment.create(orderId, PaymentMethod.CARD, 10000, null, null)

        // Then
        assertEquals(orderId, payment.orderId)
        assertEquals(PaymentMethod.CARD, payment.paymentMethod)
        assertEquals(10000, payment.amount)
        assertEquals(null, payment.paymentKey)
        assertEquals(null, payment.rawPayload)
        assertEquals(null, payment.approvedAt)
    }

    @Test
    fun `Payment canProcess 메서드 테스트`() {
        // Given - READY 상태이고 삭제되지 않은 결제
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 10000,
            paymentKey = "test_key",
            rawPayload = "{}"
        )

        // When & Then - 처리 가능해야 함
        assertTrue(payment.canProcess())

        // When - 상태를 PAID로 변경
        payment.updateStatus(PaymentStatus.PAID)

        // Then - 처리 불가능해야 함
        assertFalse(payment.canProcess())

        // When - 다시 READY로 변경하고 삭제 처리
        payment.updateStatus(PaymentStatus.READY)
        payment.softDelete()

        // Then - 삭제된 결제는 처리 불가능해야 함
        assertFalse(payment.canProcess())
    }

    @Test
    fun `Payment canCancel 메서드 테스트`() {
        // Given - 결제 생성 후 PAID 상태로 변경
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 10000,
            paymentKey = "test_key",
            rawPayload = "{}"
        )
        payment.updateStatus(PaymentStatus.PAID)

        // When & Then - 취소 가능해야 함
        assertTrue(payment.canCancel())

        // When - 상태를 READY로 변경
        payment.updateStatus(PaymentStatus.READY)

        // Then - 취소 불가능해야 함
        assertFalse(payment.canCancel())

        // When - 다시 PAID로 변경하고 삭제 처리
        payment.updateStatus(PaymentStatus.PAID)
        payment.softDelete()

        // Then - 삭제된 결제는 취소 불가능해야 함
        assertFalse(payment.canCancel())

        // When - 다른 상태들도 테스트
        val payment2 = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 20000,
            paymentKey = "test_key2",
            rawPayload = "{}"
        )

        // FAILED 상태
        payment2.updateStatus(PaymentStatus.FAILED)
        assertFalse(payment2.canCancel())

        // CANCELLED 상태
        payment2.updateStatus(PaymentStatus.CANCELLED)
        assertFalse(payment2.canCancel())
    }
}