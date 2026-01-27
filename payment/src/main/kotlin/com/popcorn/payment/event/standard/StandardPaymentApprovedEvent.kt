package com.popcorn.payment.event.standard

import java.time.LocalDateTime
import java.util.UUID

/**
 * 표준 결제 승인 이벤트 (Kotlin 버전)
 * 결제가 성공적으로 승인되었을 때 발행되는 이벤트
 */
class StandardPaymentApprovedEvent(
    /**
     * 결제 ID
     */
    var paymentId: UUID? = null,

    /**
     * 승인 시간
     */
    var approvedAt: LocalDateTime? = null,

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
         * 정적 팩토리 메서드 - Payment 도메인 객체로부터 생성
         */
        fun create(
            paymentId: UUID?,
            orderId: UUID?,
            orderNo: String?,
            userId: Long?,
            popupId: UUID?,
            amount: Int?,
            paymentMethod: String?,
            hasReservation: Boolean?,
            hasGoods: Boolean?,
            lines: List<EventLineItem>?
        ): StandardPaymentApprovedEvent {
            val event = StandardPaymentApprovedEvent(
                paymentId = paymentId,
                approvedAt = LocalDateTime.now(),
                amount = amount,
                paymentMethod = paymentMethod
            ).apply {
                this.eventType = StandardEventType.PAYMENT_APPROVED
                this.producer = "payment-service"
                this.orderId = orderId
                this.orderNo = orderNo
                this.userId = userId
                this.popupId = popupId
                this.hasReservation = hasReservation
                this.hasGoods = hasGoods
                this.lines = lines
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
        approvedAt?.let { map["approvedAt"] = it.toString() }
        amount?.let { map["amount"] = it.toString() }
        paymentMethod?.let { map["paymentMethod"] = it }

        return map
    }
}