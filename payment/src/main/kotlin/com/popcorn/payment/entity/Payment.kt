package com.popcorn.payment.entity

import com.popcorn.common.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

/**
 * 결제 엔티티
 *
 * 🗄️ 테이블 구조:
 * - id: 결제 고유 ID (UUID)
 * - orderId: 주문 ID (외래키)
 * - paymentMethod: 결제 수단 (CARD, TRANSFER, VIRTUAL_ACCOUNT 등)
 * - amount: 결제 금액
 * - status: 결제 상태 (READY, PAID, CANCELLED)
 * - approvedAt: 승인 일시
 * - rawPayload: 토스페이먼츠 원본 응답 데이터 (JSON)
 * - createdAt/updatedAt: 생성/수정 일시
 * - deletedAt: 논리 삭제 일시
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
class Payment : BaseEntity() {

    @Id
    @Column(name = "id")
    var id: UUID = UUID.randomUUID()

    @Column(name = "order_id", nullable = false)
    var orderId: UUID = UUID.randomUUID()

    @Column(name = "payment_key", length = 200, unique = true)
    var paymentKey: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    var paymentMethod: PaymentMethod = PaymentMethod.CARD

    @Column(name = "amount", nullable = false)
    var amount: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
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
        ): Payment {
            return Payment().apply {
                this.orderId = orderId
                this.paymentMethod = paymentMethod
                this.amount = amount
                this.paymentKey = paymentKey
                this.status = PaymentStatus.READY
                this.rawPayload = rawPayload
            }
        }

        /**
         * 결제 승인 완료
         */
        fun approve(
            payment: Payment,
            approvedAt: LocalDateTime,
            rawPayload: String
        ): Payment {
            return payment.apply {
                this.status = PaymentStatus.PAID
                this.approvedAt = approvedAt
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
        // BaseEntity의 updatedAt은 자동으로 관리됨
    }

    /**
     * 논리 삭제 (BaseEntity 메서드 사용)
     */
    fun softDelete() {
        this.delete() // BaseEntity의 delete() 메서드 호출
    }

    /**
     * 삭제되지 않은 결제인지 확인
     */
    fun isNotDeleted(): Boolean = !this.isDeleted() // BaseEntity의 isDeleted() 메서드 사용

    /**
     * 결제 가능한 상태인지 확인
     */
    fun canProcess(): Boolean = status == PaymentStatus.READY && isNotDeleted()

    /**
     * 취소 가능한 상태인지 확인
     */
    fun canCancel(): Boolean = status == PaymentStatus.PAID && isNotDeleted()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Payment) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Payment(id=$id, orderId=$orderId, amount=$amount, status=$status, paymentMethod=$paymentMethod)"
    }
}

/**
 * 결제 상태 열거형
 */
enum class PaymentStatus {
    READY,          // 결제 대기
    PAID,           // 결제 완료
    CANCELLED,      // 결제 취소
    FAILED          // 결제 실패
}

/**
 * 결제 수단 열거형
 */
enum class PaymentMethod {
    CARD,           // 카드
    TRANSFER,       // 계좌이체
    VIRTUAL_ACCOUNT, // 가상계좌
    MOBILE_PHONE,   // 휴대폰
    GIFT_CERTIFICATE // 상품권
}
