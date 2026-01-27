package com.popcorn.payment.event.standard

import java.time.LocalDateTime
import java.util.UUID

/**
 * 표준 결제 생성 이벤트 (Kotlin 버전)
 * 새로운 결제가 생성되었을 때 발행되는 이벤트
 */
class StandardPaymentCreatedEvent(
    /**
     * 결제 ID
     */
    var paymentId: UUID? = null,

    /**
     * 결제 상태
     */
    var paymentStatus: String? = null,

    /**
     * 결제 금액
     */
    var amount: Int? = null,

    /**
     * 결제 방법
     */
    var paymentMethod: String? = null

) : StandardBaseEvent() {

    companion object {
        /**
         * 정적 팩토리 메서드
         */
        fun create(
            paymentId: UUID?,
            orderId: UUID?,
            orderNo: String?,
            userId: Long?,
            popupId: UUID?,
            amount: Int?,
            paymentMethod: String?,
            paymentStatus: String?
        ): StandardPaymentCreatedEvent {
            val event = StandardPaymentCreatedEvent(
                paymentId = paymentId,
                paymentStatus = paymentStatus ?: "READY",
                amount = amount,
                paymentMethod = paymentMethod
            ).apply {
                this.eventType = StandardEventType.PAYMENT_CREATED
                this.producer = "payment-service"
                this.orderId = orderId
                this.orderNo = orderNo
                this.userId = userId
                this.popupId = popupId
            }

            event.setDefaults()
            return event
        }
    }

    /**
     * Redis Stream 발행용 Map 변환 (Payment 전용 필드 추가)
     */
    override fun toStreamMap(): Map<String, String> {
        val map = super.toStreamMap().toMutableMap()

        // Payment 전용 필드 추가
        paymentId?.let { map["paymentId"] = it.toString() }
        paymentStatus?.let { map["paymentStatus"] = it }
        amount?.let { map["amount"] = it.toString() }
        paymentMethod?.let { map["paymentMethod"] = it }

        return map
    }
}