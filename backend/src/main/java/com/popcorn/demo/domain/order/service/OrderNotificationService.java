package com.popcorn.demo.domain.order.service;

import org.springframework.stereotype.Component;

import com.popcorn.demo.domain.order.entity.Order;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 알림 서비스.
 * 현재는 로그로만 처리하고, 실제 환경에서는 외부 알림(SMS/Email/Push)과 연동합니다.
 */
@Component
@Slf4j
public class OrderNotificationService {

	public void notifyOrderCreated(Order order) {
		log.info("📢 주문 생성 알림 - 주문번호: {}, 고객: {}, 금액: {}원",
				order.getOrderNo(),
				order.getCustomerId(),
				order.getTotalAmount());
	}

	public void notifyOrderStatusChanged(Order order) {
		log.info("📢 주문 상태 변경 알림 - 주문번호: {}, 상태: {}, 고객: {}",
				order.getOrderNo(),
				order.getStatus(),
				order.getCustomerId());
		statusSpecificNotification(order);
	}

	public void notifyOrderCancelled(Order order) {
		log.info("📢 주문 취소 알림 - 주문번호: {}, 고객: {}, 취소시간: {}",
				order.getOrderNo(),
				order.getCustomerId(),
				order.getUpdatedAt());
	}

	private void notifyOrderCompleted(Order order) {
		log.info("✅ 주문 완료 알림 - 주문번호: {}, 주문금액: {}원",
				order.getOrderNo(),
				order.getTotalAmount());
	}

	private void statusSpecificNotification(Order order) {
		switch (order.getStatus()) {
			case COMPLETED -> notifyOrderCompleted(order);
			case CANCELLED -> notifyOrderCancelled(order);
			default -> {
				// no-op
			}
		}
	}

}
