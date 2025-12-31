package com.popcorn.demo.infrastructure.external.notification;

import org.springframework.stereotype.Component;

import com.popcorn.demo.application.order.port.out.NotifyOrderPort;
import com.popcorn.demo.domain.order.entity.Order;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 알림 어댑터 (Infrastructure Layer)
 *
 * Clean Architecture의 Adapter 패턴을 구현합니다.
 * - Application Layer의 NotifyOrderPort를 구현
 * - 외부 알림 서비스와 연동 (현재는 로깅으로 구현)
 * - 실제 환경에서는 SMS, Email, Push 알림 등과 연동
 */
@Component
@Slf4j
public class OrderNotificationAdapter implements NotifyOrderPort {

	@Override
	public void notifyOrderCreated(Order order) {
		// 실제 환경에서는 외부 알림 서비스와 연동
		log.info("📢 주문 생성 알림 - 주문번호: {}, 고객: {}, 금액: {}원",
				order.getOrderNo(),
				order.getCustomerId(),
				order.getTotalAmount());

		// 예시: 실제 알림 서비스 호출
		// smsService.sendOrderCreatedNotification(order);
		// emailService.sendOrderConfirmationEmail(order);
		// pushNotificationService.sendOrderAlert(order);
	}

	@Override
	public void notifyOrderStatusChanged(Order order) {
		log.info("📢 주문 상태 변경 알림 - 주문번호: {}, 상태: {}, 고객: {}",
				order.getOrderNo(),
				order.getStatus(),
				order.getCustomerId());

		// 예시: 상태별 차별화된 알림
		switch (order.getStatus()) {
			case COMPLETED:
				notifyOrderCompleted(order);
				break;
			case CANCELLED:
				notifyOrderCancelled(order);
				break;
			case REFUNDED:
				notifyOrderRefunded(order);
				break;
			default:
				break;
		}
	}

	@Override
	public void notifyOrderCancelled(Order order) {
		log.info("📢 주문 취소 알림 - 주문번호: {}, 고객: {}, 취소시간: {}",
				order.getOrderNo(),
				order.getCustomerId(),
				order.getUpdatedAt());
	}

	/**
	 * 주문 완료 알림
	 */
	private void notifyOrderCompleted(Order order) {
		log.info("✅ 주문 완료 알림 - 주문번호: {}, 주문금액: {}원",
				order.getOrderNo(),
				order.getTotalAmount());
	}

	/**
	 * 주문 환불 완료 알림
	 */
	private void notifyOrderRefunded(Order order) {
		log.info("💰 환불 완료 알림 - 주문번호: {}, 환불금액: {}원",
				order.getOrderNo(),
				order.getTotalAmount());
	}
}