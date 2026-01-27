package com.popcorn.payment.event.standard

import java.time.LocalDateTime

/**
 * Order 정보 응답 이벤트 (Order → Payment)
 * Order 서비스에서 Payment 서비스의 요청에 응답하는 이벤트
 */
class OrderInfoResponseEvent(
    /**
     * 원본 요청 ID (매칭용)
     */
    var requestId: String? = null,

    /**
     * 응답 성공 여부
     */
    var success: Boolean = false,

    /**
     * 실제 주문번호
     */
    var actualOrderNo: String? = null,

    /**
     * 실제 userId
     */
    var actualUserId: Long? = null,

    /**
     * 실제 popupId
     */
    var actualPopupId: String? = null,

    /**
     * 예약 포함 여부
     */
    var actualHasReservation: Boolean? = null,

    /**
     * 굿즈 포함 여부
     */
    var actualHasGoods: Boolean? = null,

    /**
     * 실제 라인 아이템들
     */
    var actualLines: List<EventLineItem>? = null,

    /**
     * 응답 시간
     */
    var respondedAt: LocalDateTime? = null

) : StandardBaseEvent() {

    /**
     * Redis Stream 발행용 Map 변환
     */
    override fun toStreamMap(): Map<String, String> {
        val map = super.toStreamMap().toMutableMap()

        requestId?.let { map["requestId"] = it }
        map["success"] = success.toString()
        actualOrderNo?.let { map["actualOrderNo"] = it }
        actualUserId?.let { map["actualUserId"] = it.toString() }
        actualPopupId?.let { map["actualPopupId"] = it }
        actualHasReservation?.let { map["actualHasReservation"] = it.toString() }
        actualHasGoods?.let { map["actualHasGoods"] = it.toString() }
        respondedAt?.let { map["respondedAt"] = it.toString() }

        // lines JSON 직렬화
        actualLines?.let { linesList ->
            try {
                val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                map["actualLines"] = objectMapper.writeValueAsString(linesList)
            } catch (e: Exception) {
                map["actualLines"] = "[]"
            }
        } ?: run {
            map["actualLines"] = "[]"
        }

        return map
    }
}