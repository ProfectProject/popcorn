package com.popcorn.payment.repository

import com.popcorn.payment.entity.PaymentStatus
import com.popcorn.payment.entity.TestPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

/**
 * 테스트용 결제 리포지터리
 */
interface TestPaymentRepository : JpaRepository<TestPayment, UUID> {

    /**
     * 주문 ID로 삭제되지 않은 결제들을 최신순으로 조회
     */
    fun findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId: UUID): List<TestPayment>

    /**
     * 결제키로 삭제되지 않은 결제들을 최신순으로 조회
     */
    @Query("SELECT p FROM TestPayment p WHERE p.rawPayload LIKE %:paymentKey% AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    fun findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(@Param("paymentKey") paymentKey: String): List<TestPayment>

    /**
     * 결제 상태로 삭제되지 않은 결제들 조회
     */
    fun findAllByStatusAndDeletedAtIsNull(status: PaymentStatus): List<TestPayment>

    /**
     * 주문 ID로 완료된 결제 금액 합계 계산
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM TestPayment p WHERE p.orderId = :orderId AND p.status = 'PAID' AND p.deletedAt IS NULL")
    fun sumPaidAmountByOrderId(@Param("orderId") orderId: UUID): Int

    /**
     * 만료된 결제들 조회
     */
    @Query("SELECT p FROM TestPayment p WHERE p.createdAt < :expiredBefore AND p.status = 'READY' AND p.deletedAt IS NULL")
    fun findExpiredPayments(@Param("expiredBefore") expiredBefore: LocalDateTime): List<TestPayment>

    /**
     * 주문에 대한 활성 결제 존재 여부 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM TestPayment p WHERE p.orderId = :orderId AND p.id != :excludePaymentId AND p.status IN ('READY', 'PAID') AND p.deletedAt IS NULL")
    fun existsActivePaymentForOrder(@Param("orderId") orderId: UUID, @Param("excludePaymentId") excludePaymentId: UUID): Boolean
}
