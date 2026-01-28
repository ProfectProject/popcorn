package com.popcorn.order.entity;

/**
 * 통합 아이템 타입 열거형
 *
 * Order와 OrderItem 모두에서 사용하는 통합 enum:
 * - RESERVATION: 예약 관련 (시간슬롯, 세션 등)
 * - GOODS: 굿즈나 상품 관련
 * - MIXED: 혼합형 (Order에서만 사용, OrderItem에서는 사용하지 않음)
 */
public enum ItemType {

    RESERVATION,  // 예약 관련
    GOODS,        // 굿즈 관련
    MIXED         // 혼합형 (Order 전용)

}