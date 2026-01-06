package com.popcorn.demo.domain.order.entity;



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
