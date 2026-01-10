package com.popcorn.demo.domain.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;

class OrderNotificationServiceTest {

	@Test
	@DisplayName("상태별 알림 분기 로직이 실행된다")
	void notifyOrderStatusChangedBranches() {
		OrderNotificationService service = new OrderNotificationService();

		Order completed = Order.builder()
				.orderNo("O-1")
				.customerId(1L)
				.status(OrderStatus.COMPLETED)
				.build();
		service.notifyOrderStatusChanged(completed);

		Order cancelled = Order.builder()
				.orderNo("O-2")
				.customerId(1L)
				.status(OrderStatus.CANCELLED)
				.build();
		service.notifyOrderStatusChanged(cancelled);

		Order requested = Order.builder()
				.orderNo("O-3")
				.customerId(1L)
				.status(OrderStatus.REQUESTED)
				.build();
		service.notifyOrderStatusChanged(requested);
	}
}
