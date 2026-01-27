package com.popcorn.payment.event.standard

import java.util.UUID

/**
 * 표준 이벤트 라인 아이템 구조 (Kotlin 버전)
 * 주문 내 개별 항목 정보 (예약/굿즈)
 */
data class EventLineItem(
    /**
     * 아이템 타입: SCHEDULE(예약) 또는 GOODS(굿즈)
     */
    val itemType: String? = null,

    // === 예약 관련 필드 (itemType = "SCHEDULE") ===
    /**
     * 스케줄 ID (예약용)
     */
    val scheduleId: UUID? = null,

    /**
     * 스케줄 시작 시간
     */
    val scheduleStartAt: String? = null,

    /**
     * 스케줄 종료 시간
     */
    val scheduleEndAt: String? = null,

    // === 굿즈 관련 필드 (itemType = "GOODS") ===
    /**
     * 굿즈 변형 ID (굿즈용)
     */
    val goodsVariantId: UUID? = null,

    /**
     * 굿즈 이름
     */
    val goodsName: String? = null,

    /**
     * 재고 단위 (예: "BLACK-M")
     */
    val stockUnit: String? = null,

    // === 공통 필드 ===
    /**
     * 주문 굿즈 ID
     */
    val orderGoodsId: UUID? = null,

    /**
     * 수량
     */
    val qty: Int? = null,

    /**
     * 단가
     */
    val unitPrice: Int? = null,

    /**
     * 라인 총 금액 (unitPrice * qty)
     */
    val linePrice: Int? = null
)