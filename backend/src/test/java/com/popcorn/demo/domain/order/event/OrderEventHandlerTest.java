package com.popcorn.demo.domain.order.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.service.OrderNotificationService;
import com.popcorn.demo.domain.order.service.OrderQueryService;

class OrderEventHandlerTest {

	@Test
	@DisplayName("주문 생성 이벤트 처리 시 저장/알림/메트릭이 수행된다")
	void handleOrderCreated() {
		OrderQueryService orderQueryService = Mockito.mock(OrderQueryService.class);
		OrderNotificationService notificationService = Mockito.mock(OrderNotificationService.class);
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics metrics = Mockito.mock(OrderEventMetrics.class);

		OrderEventHandler handler = new OrderEventHandler(
				orderQueryService,
				notificationService,
				eventStore,
				metrics
		);

		Order order = Order.builder()
				.id(UUID.randomUUID())
				.orderNo("O-1001")
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.popupId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(2000)
				.orderItems(java.util.List.of(
						OrderItem.builder()
								.id(UUID.randomUUID())
								.orderItemType(OrderItemType.RESERVATION)
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();

		OrderCreatedEvent event = new OrderCreatedEvent(order, null);
		handler.handleOrderCreated(event);

		verify(eventStore).saveEvent(event);
		verify(notificationService).notifyOrderCreated(order);
		verify(metrics).recordEventProcessed("order_created", event.getBusinessPriority());
	}

	@Test
	@DisplayName("주문 취소 이벤트 처리 시 환불/보상 경로가 실행된다")
	void handleOrderCancelled() {
		OrderQueryService orderQueryService = Mockito.mock(OrderQueryService.class);
		OrderNotificationService notificationService = Mockito.mock(OrderNotificationService.class);
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics metrics = Mockito.mock(OrderEventMetrics.class);

		OrderEventHandler handler = new OrderEventHandler(
				orderQueryService,
				notificationService,
				eventStore,
				metrics
		);

		OrderCancelledEvent event = new OrderCancelledEvent(
				UUID.randomUUID(),
				1001L,
				OrderStatus.PAID,
				"고객 취소",
				"CUSTOMER",
				15000
		);

		handler.handleOrderCancelled(event);

		verify(eventStore).saveEvent(event);
		verify(metrics).recordEventProcessed("order_cancelled", event.getCancellationSeverity());
	}

	@Test
	@DisplayName("주문 상태 변경 이벤트 처리 시 메트릭과 저장이 수행된다")
	void handleOrderStatusChanged() {
		OrderQueryService orderQueryService = Mockito.mock(OrderQueryService.class);
		OrderNotificationService notificationService = Mockito.mock(OrderNotificationService.class);
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics metrics = Mockito.mock(OrderEventMetrics.class);

		OrderEventHandler handler = new OrderEventHandler(
				orderQueryService,
				notificationService,
				eventStore,
				metrics
		);

		OrderStatusChangedEvent event = new OrderStatusChangedEvent(
				UUID.randomUUID(),
				1001L,
				OrderStatus.REQUESTED,
				OrderStatus.ACCEPTED,
				"승인",
				"OWNER"
		);

		handler.handleOrderStatusChanged(event);

		verify(eventStore).saveEvent(event);
		verify(metrics).recordEventProcessed("order_status_changed",
				event.isCriticalStatusChange() ? "HIGH" : "NORMAL");
	}
}
