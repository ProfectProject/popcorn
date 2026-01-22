package com.popcorn.payment.entity

import com.popcorn.common.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

/**
 * 테스트용 결제 엔티티 (H2 호환)
 * PostgreSQL 스키마 및 enum 의존성 제거
 */
@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payment_order_id", columnList = "order_id"),
        Index(name = "idx_payment_payment_key", columnList = "payment_key"),
        Index(name = "idx_payment_status", columnList = "status"),
        Index(name = "idx_payment_approved_at", columnList = "approved_at"),
        Index(name = "idx_payment_created_at", columnList = "created_at")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class TestPayment : BaseEntity() {

    @Id
    @Column(name = "payment_id")
    var id: UUID = UUID.randomUUID()

    @Column(name = "order_id", nullable = false)
    var orderId: UUID = UUID.randomUUID()

    @Column(name = "payment_key", length = 200, unique = true)
    var paymentKey: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    var paymentMethod: PaymentMethod = PaymentMethod.CARD

    @Column(name = "amount", nullable = false)
    var amount: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.READY

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    var rawPayload: String? = null

    companion object {
        /**
         * 새 결제 생성
         */
        fun create(
            orderId: UUID,
            paymentMethod: PaymentMethod,
            amount: Int,
            paymentKey: String? = null,
            rawPayload: String? = null
        ): TestPayment {
            return TestPayment().apply {
                this.orderId = orderId
                this.paymentMethod = paymentMethod
                this.amount = amount
                this.paymentKey = paymentKey
                this.status = PaymentStatus.READY
                this.rawPayload = rawPayload
            }
        }
    }

    /**
     * 결제 상태를 변경합니다
     */
    fun updateStatus(newStatus: PaymentStatus, approvedAt: LocalDateTime? = null) {
        this.status = newStatus
        if (approvedAt != null) {
            this.approvedAt = approvedAt
        }
    }

    /**
     * 논리 삭제 (BaseEntity 메서드 사용)
     */
    fun softDelete() {
        this.delete()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestPayment) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "TestPayment(id=$id, orderId=$orderId, amount=$amount, status=$status, paymentMethod=$paymentMethod)"
    }
}