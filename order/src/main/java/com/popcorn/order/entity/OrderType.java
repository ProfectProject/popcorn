package com.popcorn.order.entity;

/**
 * 주문 타입 열거형
 *
 * 팝콘 서비스의 주요 주문 유형을 정의합니다:
 * - RESERVATION: 예약형 주문 (시간과 장소가 정해진 예약)
 * - GOODS: 구매형 주문 (굿즈나 상품 구매)
 * - MIXED: 혼합형 주문 (예약 + 굿즈 함께)
 */
public enum OrderType {

    RESERVATION,  // 예약형 주문
    GOODS,        // 구매형 주문 (굿즈)
    MIXED         // 혼합형 주문 (예약 + 굿즈)

}