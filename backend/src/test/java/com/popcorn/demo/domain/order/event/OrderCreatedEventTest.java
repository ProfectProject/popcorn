package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;

class OrderCreatedEventTest {

    @Test
    void orderCreatedEventWithHighPriorityAmount() {
        Order order = createMockOrder(150000, OrderType.RESERVATION, OrderStatus.REQUESTED);
        OrderCreatedEvent event = new OrderCreatedEvent(order, "test-key");

        assertThat(event.getBusinessPriority()).isEqualTo("HIGH");
        assertThat(event.getTotalAmount()).isEqualTo(150000);
        assertThat(event.getOrderType()).isEqualTo(OrderType.RESERVATION);
        assertThat(event.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(event.getIdempotencyKey()).isEqualTo("test-key");
    }

    @Test
    void orderCreatedEventWithMediumPriorityAmount() {
        Order order = createMockOrder(75000, OrderType.PURCHASE, OrderStatus.ACCEPTED);
        OrderCreatedEvent event = new OrderCreatedEvent(order, "medium-key");

        assertThat(event.getBusinessPriority()).isEqualTo("MEDIUM");
        assertThat(event.getTotalAmount()).isEqualTo(75000);
        assertThat(event.getDetailedDescription()).contains("75000원");
        assertThat(event.getDetailedDescription()).contains("PURCHASE");
    }

    @Test
    void orderCreatedEventWithLowPriorityAmount() {
        Order order = createMockOrder(25000, OrderType.RESERVATION, OrderStatus.PAID);
        OrderCreatedEvent event = new OrderCreatedEvent(order, null);

        assertThat(event.getBusinessPriority()).isEqualTo("LOW");
        assertThat(event.getIdempotencyKey()).isNull();

        Map<String, Object> payload = event.getEventPayload();
        assertThat(payload).containsEntry("totalAmount", 25000);
        assertThat(payload).containsEntry("orderType", "RESERVATION");
        assertThat(payload).containsEntry("idempotencyKey", "");
    }

    @Test
    void orderCreatedEventDetailedDescription() {
        Order order = createMockOrder(50000, OrderType.PURCHASE, OrderStatus.COMPLETED);
        OrderCreatedEvent event = new OrderCreatedEvent(order, "desc-test");

        String description = event.getDetailedDescription();
        assertThat(description).contains("주문 생성됨");
        assertThat(description).contains("50000원");
        assertThat(description).contains("PURCHASE");
        assertThat(description).contains("2개");
    }

    @Test
    void orderCreatedEventPayloadStructure() {
        Order order = createMockOrder(100000, OrderType.RESERVATION, OrderStatus.RESERVED);
        OrderCreatedEvent event = new OrderCreatedEvent(order, "payload-test");

        Map<String, Object> payload = event.getEventPayload();
        assertThat(payload).containsKeys(
            "orderId", "userId", "storeId", "orderType",
            "status", "totalAmount", "itemCount", "idempotencyKey", "createdAt"
        );
        assertThat(payload.get("itemCount")).isEqualTo(2);
        assertThat(payload.get("status")).isEqualTo("RESERVED");
    }

    @Test
    void orderCreatedEventWithNullIdempotencyKey() {
        Order order = createMockOrder(30000, OrderType.PURCHASE, OrderStatus.CANCELLED);
        OrderCreatedEvent event = new OrderCreatedEvent(order, null);

        assertThat(event.getIdempotencyKey()).isNull();

        Map<String, Object> payload = event.getEventPayload();
        assertThat(payload).containsEntry("idempotencyKey", "");
    }

    private Order createMockOrder(Integer amount, OrderType type, OrderStatus status) {
        Order order = Mockito.mock(Order.class);
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        OrderItem item1 = Mockito.mock(OrderItem.class);
        OrderItem item2 = Mockito.mock(OrderItem.class);
        List<OrderItem> items = List.of(item1, item2);

        when(order.getId()).thenReturn(orderId);
        when(order.getCustomerId()).thenReturn(1001L);
        when(order.getStoreId()).thenReturn(storeId);
        when(order.getOrderType()).thenReturn(type);
        when(order.getStatus()).thenReturn(status);
        when(order.getTotalAmount()).thenReturn(amount);
        when(order.getOrderItems()).thenReturn(items);
        when(order.getCreatedAt()).thenReturn(LocalDateTime.now());

        return order;
    }
}