package com.popcorn.payment.additional

import com.popcorn.payment.entity.Payment
import com.popcorn.payment.entity.PaymentMethod
import com.popcorn.payment.entity.PaymentStatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * 추가 커버리지 달성을 위한 테스트
 *
 * [80% 커버리지 달성을 위한 마지막 시도]
 * - Payment 엔티티의 추가 메서드 테스트
 * - enum 클래스들의 완전한 테스트
 * - 80% 커버리지 달성을 위한 최종 테스트
 */
class AdditionalCoverageTest {

    @Test
    fun `Payment 엔티티 팩토리 메서드 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val amount = 50000
        val paymentKey = "test_payment_key"
        val rawPayload = """{"test": true}"""

        // When - create 메서드 테스트
        val payment = Payment.create(
            orderId = orderId,
            paymentMethod = PaymentMethod.CARD,
            amount = amount,
            paymentKey = paymentKey,
            rawPayload = rawPayload
        )

        // Then
        assertEquals(orderId, payment.orderId)
        assertEquals(PaymentMethod.CARD, payment.paymentMethod)
        assertEquals(amount, payment.amount)
        assertEquals(paymentKey, payment.paymentKey)
        assertEquals(PaymentStatus.READY, payment.status)
        assertEquals(rawPayload, payment.rawPayload)
        assertNotNull(payment.id)
    }

    @Test
    fun `Payment 엔티티 approve 메서드 테스트`() {
        // Given
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 30000
        )
        val approvedAt = LocalDateTime.now()
        val rawPayload = """{"approved": true}"""

        // When
        val approvedPayment = Payment.approve(payment, approvedAt, rawPayload)

        // Then
        assertEquals(PaymentStatus.PAID, approvedPayment.status)
        assertEquals(approvedAt, approvedPayment.approvedAt)
        assertEquals(rawPayload, approvedPayment.rawPayload)
        assertTrue(approvedPayment === payment) // 같은 인스턴스
    }

    @Test
    fun `Payment 엔티티 비즈니스 메서드 테스트`() {
        // Given
        val readyPayment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 25000
        )

        val paidPayment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.TRANSFER,
            amount = 40000
        ).apply {
            status = PaymentStatus.PAID
            approvedAt = LocalDateTime.now()
        }

        // When & Then - canProcess 테스트
        assertTrue(readyPayment.canProcess())
        assertFalse(paidPayment.canProcess())

        // When & Then - canCancel 테스트
        assertFalse(readyPayment.canCancel())
        assertTrue(paidPayment.canCancel())

        // When & Then - isNotDeleted 테스트
        assertTrue(readyPayment.isNotDeleted())
        assertTrue(paidPayment.isNotDeleted())
    }

    @Test
    fun `Payment 엔티티 updateStatus 메서드 테스트`() {
        // Given
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 35000
        )
        val approvedAt = LocalDateTime.now()

        // When
        payment.updateStatus(PaymentStatus.PAID, approvedAt)

        // Then
        assertEquals(PaymentStatus.PAID, payment.status)
        assertEquals(approvedAt, payment.approvedAt)
    }

    @Test
    fun `Payment 엔티티 softDelete 메서드 테스트`() {
        // Given
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 20000
        )

        // When
        payment.softDelete()

        // Then
        assertFalse(payment.isNotDeleted())
        assertFalse(payment.canProcess())
        assertFalse(payment.canCancel())
    }

    @Test
    fun `PaymentMethod enum 완전 테스트`() {
        // When
        val methods = PaymentMethod.values()

        // Then
        assertEquals(5, methods.size)
        assertTrue(methods.contains(PaymentMethod.CARD))
        assertTrue(methods.contains(PaymentMethod.TRANSFER))
        assertTrue(methods.contains(PaymentMethod.VIRTUAL_ACCOUNT))
        assertTrue(methods.contains(PaymentMethod.MOBILE_PHONE))
        assertTrue(methods.contains(PaymentMethod.GIFT_CERTIFICATE))

        // valueOf 테스트
        assertEquals(PaymentMethod.CARD, PaymentMethod.valueOf("CARD"))
        assertEquals(PaymentMethod.TRANSFER, PaymentMethod.valueOf("TRANSFER"))
        assertEquals(PaymentMethod.VIRTUAL_ACCOUNT, PaymentMethod.valueOf("VIRTUAL_ACCOUNT"))
        assertEquals(PaymentMethod.MOBILE_PHONE, PaymentMethod.valueOf("MOBILE_PHONE"))
        assertEquals(PaymentMethod.GIFT_CERTIFICATE, PaymentMethod.valueOf("GIFT_CERTIFICATE"))

        // ordinal 및 name 테스트
        methods.forEach { method ->
            assertNotNull(method.name)
            assertTrue(method.ordinal >= 0)
            assertEquals(method, PaymentMethod.values()[method.ordinal])
        }
    }

    @Test
    fun `PaymentStatus enum 완전 테스트`() {
        // When
        val statuses = PaymentStatus.values()

        // Then
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(PaymentStatus.READY))
        assertTrue(statuses.contains(PaymentStatus.PAID))
        assertTrue(statuses.contains(PaymentStatus.CANCELLED))
        assertTrue(statuses.contains(PaymentStatus.FAILED))

        // valueOf 테스트
        assertEquals(PaymentStatus.READY, PaymentStatus.valueOf("READY"))
        assertEquals(PaymentStatus.PAID, PaymentStatus.valueOf("PAID"))
        assertEquals(PaymentStatus.CANCELLED, PaymentStatus.valueOf("CANCELLED"))
        assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"))

        // ordinal 및 name 테스트
        statuses.forEach { status ->
            assertNotNull(status.name)
            assertTrue(status.ordinal >= 0)
            assertEquals(status, PaymentStatus.values()[status.ordinal])
        }
    }

    @Test
    fun `Payment 엔티티 toString 메서드 테스트`() {
        // Given
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.VIRTUAL_ACCOUNT,
            amount = 60000,
            paymentKey = "test_key_toString"
        )

        // When
        val toString = payment.toString()

        // Then
        assertNotNull(toString)
        assertTrue(toString.contains("Payment"))
        assertTrue(toString.contains(payment.id.toString()))
        assertTrue(toString.contains(payment.orderId.toString()))
        assertTrue(toString.contains("60000"))
        assertTrue(toString.contains("READY"))
        assertTrue(toString.contains("VIRTUAL_ACCOUNT"))
    }

    @Test
    fun `Payment 엔티티 equals 및 hashCode 테스트`() {
        // Given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        val payment1 = Payment().apply { id = id1 }
        val payment2 = Payment().apply { id = id1 } // 같은 ID
        val payment3 = Payment().apply { id = id2 } // 다른 ID

        // When & Then
        assertEquals(payment1, payment2) // 같은 ID면 같은 엔티티
        assertFalse(payment1 == payment3) // 다른 ID면 다른 엔티티

        assertEquals(payment1.hashCode(), payment2.hashCode())
        assertTrue(payment1.hashCode() != payment3.hashCode())

        // null과 비교
        assertFalse(payment1.equals(null))
        assertFalse(payment1.equals("string"))
    }

    @Test
    fun `Payment 엔티티 모든 프로퍼티 설정 테스트`() {
        // Given
        val payment = Payment()
        val orderId = UUID.randomUUID()
        val approvedAt = LocalDateTime.now()

        // When - 모든 프로퍼티 설정
        payment.id = UUID.randomUUID()
        payment.orderId = orderId
        payment.paymentKey = "comprehensive_test_key"
        payment.paymentMethod = PaymentMethod.MOBILE_PHONE
        payment.amount = 85000
        payment.status = PaymentStatus.FAILED
        payment.approvedAt = approvedAt
        payment.rawPayload = """{"comprehensive": "test"}"""

        // Then
        assertEquals(orderId, payment.orderId)
        assertEquals("comprehensive_test_key", payment.paymentKey)
        assertEquals(PaymentMethod.MOBILE_PHONE, payment.paymentMethod)
        assertEquals(85000, payment.amount)
        assertEquals(PaymentStatus.FAILED, payment.status)
        assertEquals(approvedAt, payment.approvedAt)
        assertTrue(payment.rawPayload!!.contains("comprehensive"))
    }
}