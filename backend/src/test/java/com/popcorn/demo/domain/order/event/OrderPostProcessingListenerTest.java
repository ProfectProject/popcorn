package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.popcorn.demo.domain.order.service.OrderService;

class OrderPostProcessingListenerTest {

	@Test
	@DisplayName("Processes post actions and sends notification")
	void handleProcessesOrder() {
		OrderService orderService = Mockito.mock(OrderService.class);
		OrderNotificationService notificationService = Mockito.mock(OrderNotificationService.class);
		OrderPostProcessingListener listener = new OrderPostProcessingListener(orderService, notificationService);

		Order order = Order.builder()
				.id(UUID.randomUUID())
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.popupId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.orderItems(List.of(OrderItem.builder()
						.orderItemType(OrderItemType.RESERVATION)
						.qty(1)
						.unitPrice(10000)
						.lineAmount(10000)
						.build()))
				.build();
		OrderCreatedEvent event = new OrderCreatedEvent(order, null);

		listener.handle(event);

		verify(orderService).processOrderPostActions(order.getId());
		verify(notificationService).notifyOrderCreated(order);
	}

	@Test
	@DisplayName("Swallows runtime exception during post processing")
	void handleCatchesRuntimeException() {
		OrderService orderService = Mockito.mock(OrderService.class);
		OrderNotificationService notificationService = Mockito.mock(OrderNotificationService.class);
		OrderPostProcessingListener listener = new OrderPostProcessingListener(orderService, notificationService);

		Order order = Order.builder()
				.id(UUID.randomUUID())
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.popupId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.orderItems(List.of())
				.build();
		OrderCreatedEvent event = new OrderCreatedEvent(order, null);
		Mockito.doThrow(new RuntimeException("fail"))
				.when(orderService).processOrderPostActions(order.getId());

		assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
		verify(notificationService, never()).notifyOrderCreated(order);
	}
}
