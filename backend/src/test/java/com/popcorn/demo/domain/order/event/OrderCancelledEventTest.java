package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderStatus;

class OrderCancelledEventTest {

    @Test
    void orderCancelledEventWithCustomerCancellation() {
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
            orderId, 1001L, OrderStatus.ACCEPTED, "Customer changed mind",
            "123", 50000
        );

        assertThat(event.getPreviousStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(event.getCancellationReason()).isEqualTo("Customer changed mind");
        assertThat(event.getCancelledBy()).isEqualTo("123");
        assertThat(event.getRefundAmount()).isEqualTo(50000);
        assertThat(event.isRefundRequired()).isTrue();
    }

    @Test
    void orderCancelledEventPayload() {
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
            orderId, 1001L, OrderStatus.PAID, "Payment failed",
            "SYSTEM", 75000
        );

        Map<String, Object> payload = event.getEventPayload();
        assertThat(payload).containsEntry("previousStatus", "PAID");
        assertThat(payload).containsEntry("cancellationReason", "Payment failed");
        assertThat(payload).containsEntry("cancelledBy", "SYSTEM");
        assertThat(payload).containsEntry("refundAmount", 75000);
        assertThat(payload).containsEntry("isRefundRequired", true);
    }

    @Test
    void orderCancelledEventWithNullValues() {
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
            orderId, 1001L, OrderStatus.REQUESTED, null,
            null, 0
        );

        assertThat(event.getCancellationReason()).isNull();
        assertThat(event.getCancelledBy()).isEqualTo("SYSTEM");
        assertThat(event.getRefundAmount()).isEqualTo(0);
        assertThat(event.isRefundRequired()).isFalse();
    }
}