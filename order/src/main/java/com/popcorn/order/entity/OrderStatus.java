package com.popcorn.order.entity;

/**
 * 주문 상태 열거형
 *
 * 주문의 생명주기를 나타내는 상태값들입니다.
 * 각 상태는 특정한 비즈니스 의미를 가지며, 상태 전이는 비즈니스 규칙에 따라 제한됩니다.
 */
public enum OrderStatus {

    REQUESTED,       // 주문 요청됨
    ACCEPTED,        // 주문 수락됨
    REJECTED,        // 주문 거절됨
    RESERVED,        // 예약 확정됨
    PAYMENT_PENDING, // 결제 대기
    PAID,            // 결제 완료됨
    COMPLETED,       // 완료
    CANCELLED        // 취소됨

}