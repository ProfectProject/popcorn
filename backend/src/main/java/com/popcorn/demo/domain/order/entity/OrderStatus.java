package com.popcorn.demo.domain.order.entity;



public enum OrderStatus {

	REQUESTED,     // 주문 요청됨

	OWNER_ACCEPTED, // 운영 수락됨

	OWNER_REJECTED, // 운영 거절됨

	CONFIRMED,     // 주문 확인됨

	PREPARING,     // 준비 중

	READY,         // 준비 완료

	COMPLETED,     // 완료

	CANCELLED,     // 취소됨

	REFUNDED       // 환불됨

}
