package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;

class OrderEventStoreTest {

	@Test
	@DisplayName("이벤트 저장/조회 및 통계 조회가 가능하다")
	void saveAndQueryEvents() {
		OrderEventStore store = new OrderEventStore(objectMapper());

		UUID orderId = UUID.randomUUID();
		Order order = createOrder(orderId);
		OrderCreatedEvent createdEvent = new OrderCreatedEvent(order, "idem-key");
		OrderStatusChangedEvent statusChangedEvent = new OrderStatusChangedEvent(
				orderId,
				order.getCustomerId(),
				OrderStatus.REQUESTED,
				OrderStatus.ACCEPTED,
				"approve",
				"SYSTEM"
		);

		store.saveEvent(createdEvent);
		store.saveEvent(statusChangedEvent);

		List<OrderEventStore.EventRecord> stream = store.getEventStream(orderId);
		assertThat(stream).hasSize(2);

		List<OrderEventStore.EventRecord> createdEvents = store.getEventsByType("order_created");
		assertThat(createdEvents).hasSize(1);

		List<OrderEventStore.EventRecord> recent = store.getRecentEvents(1);
		assertThat(recent).hasSize(1);

		OrderEventStore.EventStoreStats stats = store.getStatistics();
		assertThat(stats.getTotalEvents()).isEqualTo(2);
		assertThat(stats.getTotalOrderStreams()).isEqualTo(1);
		assertThat(stats.getEventTypeCounts()).containsKeys("order_created", "order_status_changed");
	}

	@Test
	@DisplayName("시간 범위 조회와 정리가 동작한다")
	void rangeQueryAndCleanup() {
		OrderEventStore store = new OrderEventStore(objectMapper());
		UUID orderId = UUID.randomUUID();
		Order order = createOrder(orderId);

		OrderCreatedEvent createdEvent = new OrderCreatedEvent(order, null);
		store.saveEvent(createdEvent);

		LocalDateTime from = LocalDateTime.now().minusMinutes(1);
		LocalDateTime to = LocalDateTime.now().plusMinutes(1);

		assertThat(store.getEventStream(orderId, from, to)).hasSize(1);

		store.cleanup(LocalDateTime.now().plusMinutes(2));
		assertThat(store.getEventStream(orderId)).isEmpty();
	}

	private Order createOrder(UUID orderId) {
		Order order = Order.builder()
				.id(orderId)
				.orderNo("O-1001")
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.popupId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(2000)
				.orderItems(List.of(
						OrderItem.builder()
								.id(UUID.randomUUID())
								.orderItemType(OrderItemType.RESERVATION)
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();
		order.setCreatedAt(LocalDateTime.now());
		return order;
	}

	private ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
