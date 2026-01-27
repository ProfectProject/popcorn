package com.popcorn.payment.event.standard

import java.time.LocalDateTime
import java.util.UUID

/**
 * 표준 이벤트 기본 구조 (Kotlin 버전)
 * 모든 표준 이벤트가 상속해야 하는 기본 클래스
 */
abstract class StandardBaseEvent(
    // === 📦 공통 Event Payload (모든 이벤트 공통) ===

    /**
     * 이벤트 고유 ID
     */
    var eventId: String? = null,

    /**
     * 이벤트 타입 (ORDER_CREATED, PAYMENT_APPROVED 등)
     */
    var eventType: StandardEventType? = null,

    /**
     * 이벤트 발생 시간
     */
    var occurredAt: LocalDateTime? = null,

    /**
     * 이벤트 생산자 서비스 (order-service, payment-service 등)
     */
    var producer: String? = null,

    // === 비즈니스 공통 필드 ===

    /**
     * 주문 ID
     */
    var orderId: UUID? = null,

    /**
     * 주문 번호
     */
    var orderNo: String? = null,

    /**
     * 사용자 ID
     */
    var userId: Long? = null,

    /**
     * 상점 ID
     */
    var storeId: UUID? = null,

    /**
     * 팝업 ID
     */
    var popupId: UUID? = null,

    /**
     * 예약 포함 여부
     */
    var hasReservation: Boolean? = null,

    /**
     * 굿즈 포함 여부
     */
    var hasGoods: Boolean? = null,

    /**
     * 라인 아이템 목록 (주문 내 개별 항목들)
     */
    var lines: List<EventLineItem>? = null
) {

    /**
     * 기본값으로 현재 시간과 UUID 설정
     */
    protected fun setDefaults() {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString()
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now()
        }
    }

    /**
     * Redis Stream 발행용 Map 변환
     */
    open fun toStreamMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()

        // 공통 필드
        eventId?.let { map["eventId"] = it }
        eventType?.let { map["eventType"] = it.value }
        occurredAt?.let { map["occurredAt"] = it.toString() }
        producer?.let { map["producer"] = it }

        // 비즈니스 필드
        orderId?.let { map["orderId"] = it.toString() }
        orderNo?.let { map["orderNo"] = it }
        userId?.let { map["userId"] = it.toString() }
        storeId?.let { map["storeId"] = it.toString() }
        popupId?.let { map["popupId"] = it.toString() }
        hasReservation?.let { map["hasReservation"] = it.toString() }
        hasGoods?.let { map["hasGoods"] = it.toString() }

        // lines JSON 직렬화
        lines?.let { linesList ->
            try {
                val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                map["lines"] = objectMapper.writeValueAsString(linesList)
            } catch (e: Exception) {
                map["lines"] = "[]"
            }
        } ?: run {
            map["lines"] = "[]"
        }

        return map
    }
}