package com.popcorn.payment.repository

import com.popcorn.payment.entity.Payment
import com.popcorn.payment.entity.PaymentMethod
import com.popcorn.payment.entity.PaymentStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Test
    fun `결제 저장 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payment = Payment.create(
            orderId = orderId,
            paymentMethod = PaymentMethod.CARD,
            amount = 10000,
            rawPayload = """{"paymentKey":"test_key"}"""
        )

        // When
        val saved = paymentRepository.save(payment)
        entityManager.flush()

        // Then
        assertNotNull(saved.id)
        assertEquals(orderId, saved.orderId)
        assertEquals(PaymentMethod.CARD, saved.paymentMethod)
        assertEquals(10000, saved.amount)
        assertEquals(PaymentStatus.READY, saved.status)
    }

    @Test
    fun `주문 ID로 결제 조회 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payment1 = Payment.create(orderId, PaymentMethod.CARD, 10000)
        val payment2 = Payment.create(orderId, PaymentMethod.TRANSFER, 15000)

        paymentRepository.save(payment1)
        paymentRepository.save(payment2)
        entityManager.flush()

        // When
        val payments = paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId)

        // Then
        assertEquals(2, payments.size)
        assertEquals(orderId, payments[0].orderId)
        assertEquals(orderId, payments[1].orderId)
    }

    @Test
    fun `결제 키로 결제 조회 테스트`() {
        // Given
        val paymentKey = "test_payment_key_12345"
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            paymentMethod = PaymentMethod.CARD,
            amount = 10000,
            rawPayload = """{"paymentKey":"$paymentKey","amount":10000}"""
        )

        paymentRepository.save(payment)
        entityManager.flush()

        // When
        val payments = paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(paymentKey)

        // Then
        assertEquals(1, payments.size)
        assertTrue(payments[0].rawPayload?.contains(paymentKey) == true)
    }

    @Test
    fun `결제 상태별 조회 테스트`() {
        // Given
        val payment1 = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000)
        payment1.status = PaymentStatus.PAID

        val payment2 = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 15000)
        payment2.status = PaymentStatus.READY

        paymentRepository.saveAll(listOf(payment1, payment2))
        entityManager.flush()

        // When
        val paidPayments = paymentRepository.findAllByStatusAndDeletedAtIsNull(PaymentStatus.PAID)
        val readyPayments = paymentRepository.findAllByStatusAndDeletedAtIsNull(PaymentStatus.READY)

        // Then
        assertEquals(1, paidPayments.size)
        assertEquals(PaymentStatus.PAID, paidPayments[0].status)
        assertEquals(1, readyPayments.size)
        assertEquals(PaymentStatus.READY, readyPayments[0].status)
    }

    @Test
    fun `결제 금액 합계 계산 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payment1 = Payment.create(orderId, PaymentMethod.CARD, 10000)
        payment1.status = PaymentStatus.PAID

        val payment2 = Payment.create(orderId, PaymentMethod.CARD, 15000)
        payment2.status = PaymentStatus.PAID

        val payment3 = Payment.create(orderId, PaymentMethod.CARD, 5000)
        payment3.status = PaymentStatus.READY // PAID가 아님

        paymentRepository.saveAll(listOf(payment1, payment2, payment3))
        entityManager.flush()

        // When
        val totalAmount = paymentRepository.sumPaidAmountByOrderId(orderId)

        // Then
        assertEquals(25000, totalAmount) // 10000 + 15000 (READY는 제외)
    }

    @Test
    fun `만료된 결제 조회 테스트`() {
        // Given
        val oldPayment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000)
        oldPayment.createdAt = LocalDateTime.now().minusHours(2) // 2시간 전

        val newPayment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 15000)
        newPayment.createdAt = LocalDateTime.now().minusMinutes(10) // 10분 전

        paymentRepository.saveAll(listOf(oldPayment, newPayment))
        entityManager.flush()

        // When
        val expiredPayments = paymentRepository.findExpiredPayments(LocalDateTime.now().minusHours(1))

        // Then
        assertEquals(1, expiredPayments.size)
        assertEquals(oldPayment.id, expiredPayments[0].id)
    }

    @Test
    fun `중복 결제 검증 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payment1 = Payment.create(orderId, PaymentMethod.CARD, 10000)
        payment1.status = PaymentStatus.PAID

        val payment2 = Payment.create(orderId, PaymentMethod.CARD, 15000)
        payment2.status = PaymentStatus.READY

        val savedPayment1 = paymentRepository.save(payment1)
        paymentRepository.save(payment2)
        entityManager.flush()

        // When
        val hasActivePayment = paymentRepository.existsActivePaymentForOrder(
            orderId, savedPayment1.id
        )

        // Then
        assertTrue(hasActivePayment) // payment2가 READY 상태로 존재
    }

    @Test
    fun `논리 삭제된 결제 제외 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val payment1 = Payment.create(orderId, PaymentMethod.CARD, 10000)
        val payment2 = Payment.create(orderId, PaymentMethod.CARD, 15000)
        payment2.softDelete() // 논리 삭제

        paymentRepository.saveAll(listOf(payment1, payment2))
        entityManager.flush()

        // When
        val activePayments = paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId)

        // Then
        assertEquals(1, activePayments.size)
        assertEquals(payment1.id, activePayments[0].id)
    }
}