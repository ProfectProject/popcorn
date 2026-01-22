package com.popcorn.payment.event

import java.time.LocalDateTime
import java.util.*

/**
 * 결제 관련 이벤트 정의
 *
 * 🎪 이벤트 기반 아키텍처:
 * - 도메인 간 느슨한 결합
 * - 비동기 후속 처리
 * - 확장성과 유지보수성 향상
 * - 트랜잭션 경계 분리
 */

/**
 * 결제 관련 이벤트의 기본 인터페이스
 */
sealed interface PaymentEvent {
    val paymentId: UUID
    val orderId: UUID
    val occurredAt: LocalDateTime
}

/**
 * 결제 생성 이벤트
 * 새로운 결제가 생성되었을 때 발행
 */
data class PaymentCreatedEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val paymentMethod: String,
    val customerId: Long?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 결제 승인 이벤트
 * 결제가 성공적으로 승인되었을 때 발행
 */
data class PaymentApprovedEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val paymentMethod: String,
    val paymentKey: String?,
    val approvedAt: LocalDateTime,
    val customerId: Long?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 결제 실패 이벤트
 * 결제 승인이 실패했을 때 발행
 */
data class PaymentFailedEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val paymentMethod: String,
    val failureReason: String,
    val customerId: Long?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 결제 취소 이벤트
 * 결제가 취소되었을 때 발행
 */
data class PaymentCancelledEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val cancelAmount: Int,
    val cancelReason: String,
    val customerId: Long?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 결제 취소 실패 이벤트
 * 결제 취소가 실패했을 때 발행 (재시도 큐에 추가)
 */
data class PaymentCancelFailedEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val cancelReason: String,
    val failureReason: String,
    val retryCount: Int = 0,
    val customerId: Long?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 결제 만료 이벤트
 * READY 상태로 오래 방치된 결제를 정리할 때 발행
 */
data class PaymentExpiredEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val expiredAt: LocalDateTime,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 결제 성공 이벤트 (통합)
 * 결제 승인 완료 후 후속 처리를 위해 발행하는 통합 이벤트
 */
data class PaymentSuccessEvent(
    override val paymentId: UUID,
    override val orderId: UUID,
    val orderNo: String,
    val orderType: String, // PURCHASE, RESERVATION
    val totalAmount: Int,
    val userId: Long?,
    val orderItems: List<OrderItemInfo>?,
    val paidAt: LocalDateTime,
    val paymentMethod: String,
    val paymentKey: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : PaymentEvent

/**
 * 주문 항목 정보 (이벤트용 DTO)
 */
data class OrderItemInfo(
    val id: UUID,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: Int,
    val itemType: String // PRODUCT, RESERVATION 등
)

/**
 * 이벤트 발행을 위한 Publisher 인터페이스
 */
interface PaymentEventPublisher {
    suspend fun publish(event: PaymentEvent)
    suspend fun publishAll(events: List<PaymentEvent>)
}

/**
 * 이벤트 핸들러 기본 인터페이스
 */
interface PaymentEventHandler<T : PaymentEvent> {
    suspend fun handle(event: T)
    val eventType: Class<T>
}