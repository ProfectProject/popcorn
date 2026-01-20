package com.popcorn.payment.repository

import com.popcorn.payment.entity.Payment
import com.popcorn.payment.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * 결제 정보 JPA Repository
 *
 * 🗄️ 주요 기능:
 * - 기본 CRUD 연산
 * - 주문 ID 기반 결제 조회
 * - PaymentKey 기반 결제 조회 (토스페이먼츠 연동)
 * - 상태별 결제 조회
 * - 논리 삭제 지원
 */
@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {

    /**
     * 주문 ID로 삭제되지 않은 결제 목록 조회 (최신순)
     *
     * @param orderId 주문 ID
     * @return 결제 목록
     */
    fun findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId: UUID): List<Payment>

    /**
     * 주문 ID로 가장 최신 결제 조회
     *
     * @param orderId 주문 ID
     * @return 최신 결제 정보
     */
    fun findFirstByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId: UUID): Payment?

    /**
     * 결제 상태로 결제 목록 조회
     *
     * @param status 결제 상태
     * @return 결제 목록
     */
    fun findAllByStatusAndDeletedAtIsNull(status: PaymentStatus): List<Payment>

    /**
     * 특정 기간 내 결제 조회
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 결제 목록
     */
    fun findAllByCreatedAtBetweenAndDeletedAtIsNull(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Payment>

    /**
     * 결제 키(PaymentKey)로 결제 조회
     * rawPayload에서 paymentKey를 검색하여 조회
     *
     * @param paymentKey 토스페이먼츠 결제 키
     * @return 결제 목록
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.rawPayload IS NOT NULL
        AND p.rawPayload LIKE %:paymentKey%
        AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
    """)
    fun findByPaymentKeyInRawPayload(@Param("paymentKey") paymentKey: String): List<Payment>

    /**
     * 승인 완료된 결제 중 특정 금액으로 조회
     *
     * @param amount 결제 금액
     * @return 결제 목록
     */
    fun findAllByStatusAndAmountAndDeletedAtIsNull(
        status: PaymentStatus,
        amount: Int
    ): List<Payment>

    /**
     * 특정 주문의 승인된 결제 총 금액 계산
     *
     * @param orderId 주문 ID
     * @return 승인된 결제 총 금액
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.orderId = :orderId
        AND p.status = 'PAID'
        AND p.deletedAt IS NULL
    """)
    fun sumPaidAmountByOrderId(@Param("orderId") orderId: UUID): Int

    /**
     * 실패한 결제 목록 조회 (재시도 대상)
     *
     * @param beforeDate 특정 시간 이전
     * @return 실패한 결제 목록
     */
    fun findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
        status: PaymentStatus,
        beforeDate: LocalDateTime
    ): List<Payment>

    /**
     * 결제 통계 조회 - 일별 결제 건수
     *
     * @param date 조회 날짜
     * @return 결제 건수
     */
    @Query("""
        SELECT COUNT(p)
        FROM Payment p
        WHERE DATE(p.createdAt) = DATE(:date)
        AND p.status = 'PAID'
        AND p.deletedAt IS NULL
    """)
    fun countPaidPaymentsByDate(@Param("date") date: LocalDateTime): Long

    /**
     * 결제 통계 조회 - 일별 결제 금액 합계
     *
     * @param date 조회 날짜
     * @return 결제 금액 합계
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE DATE(p.createdAt) = DATE(:date)
        AND p.status = 'PAID'
        AND p.deletedAt IS NULL
    """)
    fun sumPaidAmountByDate(@Param("date") date: LocalDateTime): Long

    /**
     * 결제 수단별 통계 조회
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 결제 수단별 통계 (결제수단, 건수, 금액)
     */
    @Query("""
        SELECT p.paymentMethod, COUNT(p), SUM(p.amount)
        FROM Payment p
        WHERE p.createdAt BETWEEN :startDate AND :endDate
        AND p.status = 'PAID'
        AND p.deletedAt IS NULL
        GROUP BY p.paymentMethod
    """)
    fun getPaymentStatsByMethod(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * 중복 결제 검증 - 동일 주문에 대한 진행 중인 결제 확인
     *
     * @param orderId 주문 ID
     * @param excludePaymentId 제외할 결제 ID (현재 결제 제외)
     * @return 진행 중인 결제 존재 여부
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM Payment p
        WHERE p.orderId = :orderId
        AND p.status IN ('READY', 'PAID')
        AND p.id != :excludePaymentId
        AND p.deletedAt IS NULL
    """)
    fun existsActivePaymentForOrder(
        @Param("orderId") orderId: UUID,
        @Param("excludePaymentId") excludePaymentId: UUID
    ): Boolean

    /**
     * 만료된 결제 조회 (READY 상태로 오래 방치된 결제)
     *
     * @param beforeDate 만료 기준 시간
     * @return 만료된 결제 목록
     */
    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.status = 'READY'
        AND p.createdAt < :beforeDate
        AND p.deletedAt IS NULL
    """)
    fun findExpiredPayments(@Param("beforeDate") beforeDate: LocalDateTime): List<Payment>

    /**
     * 사용자별 결제 내역 조회 (주문 ID를 통한 간접 조회)
     * 실제 구현 시에는 Order 테이블과 JOIN 필요
     *
     * @param orderIds 사용자의 주문 ID 목록
     * @return 결제 내역
     */
    fun findAllByOrderIdInAndDeletedAtIsNullOrderByCreatedAtDesc(orderIds: List<UUID>): List<Payment>
}