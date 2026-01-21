package com.popcorn.order.entity;

/**
 * 주문 항목 타입 열거형
 *
 * 주문에 포함되는 항목의 종류를 구분합니다:
 * - RESERVATION: 예약 관련 항목 (시간슬롯, 세션 등)
 * - GOODS: 굿즈나 상품 항목
 */
public enum OrderItemType {

    RESERVATION,  // 예약 항목
    GOODS         // 굿즈 항목

}