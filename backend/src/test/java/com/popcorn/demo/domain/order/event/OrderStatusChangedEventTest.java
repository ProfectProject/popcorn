package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderStatus;

class OrderStatusChangedEventTest {

    @Test
    void orderStatusChangedEventWithCriticalStatus() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.PAID, OrderStatus.COMPLETED, "Order fulfilled", "123"
        );

        assertThat(event.getFromStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(event.getToStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(event.getReason()).isEqualTo("Order fulfilled");
        assertThat(event.getChangedBy()).isEqualTo("123");
        assertThat(event.getChangeType()).isEqualTo("MANUAL");

        assertThat(event.isCriticalStatusChange()).isTrue();
        assertThat(event.requiresCustomerNotification()).isTrue();
        assertThat(event.isReversibleChange()).isFalse();
        assertThat(event.isStatusProgression()).isTrue();
    }

    @Test
    void orderStatusChangedEventWithSystemChange() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.REQUESTED, OrderStatus.ACCEPTED, null, null
        );

        assertThat(event.getChangedBy()).isEqualTo("SYSTEM");
        assertThat(event.getChangeType()).isEqualTo("SYSTEM");
        assertThat(event.getReason()).isNull();

        assertThat(event.isCriticalStatusChange()).isTrue();
        assertThat(event.requiresCustomerNotification()).isTrue();
        assertThat(event.isReversibleChange()).isTrue();
        assertThat(event.isStatusProgression()).isTrue();
    }

    @Test
    void orderStatusChangedEventWithAutomaticChange() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.RESERVED, OrderStatus.PAYMENT_PENDING, "Timer triggered", "AUTO_SYSTEM"
        );

        assertThat(event.getChangeType()).isEqualTo("AUTOMATIC");
        assertThat(event.isCriticalStatusChange()).isFalse();
        assertThat(event.requiresCustomerNotification()).isFalse();
        assertThat(event.isReversibleChange()).isTrue();
        assertThat(event.isStatusProgression()).isTrue();
    }

    @Test
    void orderStatusChangedEventWithCancellation() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.ACCEPTED, OrderStatus.CANCELLED, "Customer request", "456"
        );

        assertThat(event.isCriticalStatusChange()).isTrue();
        assertThat(event.requiresCustomerNotification()).isTrue();
        assertThat(event.isReversibleChange()).isFalse();
        assertThat(event.isStatusProgression()).isTrue();

        String description = event.getStatusChangeDescription();
        assertThat(description).contains("승인됨 → 취소됨");
        assertThat(description).contains("사유: Customer request");
        assertThat(description).contains("변경자: 456");
    }

    @Test
    void orderStatusChangedEventWithRejection() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.REQUESTED, OrderStatus.REJECTED, "Out of stock", "789"
        );

        assertThat(event.isCriticalStatusChange()).isTrue();
        assertThat(event.requiresCustomerNotification()).isTrue();
        assertThat(event.isReversibleChange()).isFalse();
        assertThat(event.isStatusProgression()).isTrue();
    }

    @Test
    void orderStatusChangedEventWithNonCriticalStatus() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.ACCEPTED, OrderStatus.RESERVED, "Table assigned", "WAITER_SYSTEM"
        );

        assertThat(event.isCriticalStatusChange()).isFalse();
        assertThat(event.requiresCustomerNotification()).isFalse();
        assertThat(event.isReversibleChange()).isTrue();
        assertThat(event.isStatusProgression()).isTrue();
    }

    @Test
    void orderStatusChangedEventPayload() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, "Payment processed", "PAYMENT_GATEWAY"
        );

        Map<String, Object> payload = event.getEventPayload();
        assertThat(payload).containsEntry("fromStatus", "PAYMENT_PENDING");
        assertThat(payload).containsEntry("toStatus", "PAID");
        assertThat(payload).containsEntry("reason", "Payment processed");
        assertThat(payload).containsEntry("changedBy", "PAYMENT_GATEWAY");
        assertThat(payload).containsEntry("changeType", "AUTOMATIC");
        assertThat(payload).containsEntry("isProgression", true);
        assertThat(payload).containsEntry("isCritical", false);
        assertThat(payload).containsEntry("requiresNotification", true);
    }

    @Test
    void orderStatusChangedEventDescriptionWithoutReason() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.RESERVED, OrderStatus.CANCELLED, "", "999"
        );

        String description = event.getStatusChangeDescription();
        assertThat(description).contains("예약됨 → 취소됨");
        assertThat(description).doesNotContain("사유:");
        assertThat(description).contains("변경자: 999");
    }

    @Test
    void orderStatusChangedEventToString() {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.REQUESTED, OrderStatus.ACCEPTED, "Valid request", "admin"
        );

        String toString = event.toString();
        assertThat(toString).contains("OrderStatusChangedEvent");
        assertThat(toString).contains("REQUESTED → ACCEPTED");
        assertThat(toString).contains("changedBy=admin");
        assertThat(toString).contains("orderId=" + orderId.toString());
    }

    @Test
    void orderStatusChangedEventWithNonProgressionChange() {
        UUID orderId = UUID.randomUUID();
        // This represents a non-progression change (payment pending back to reserved)
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, 1001L, OrderStatus.PAYMENT_PENDING, OrderStatus.RESERVED, "Payment issue", "SYSTEM"
        );

        assertThat(event.isStatusProgression()).isFalse();
        assertThat(event.isReversibleChange()).isTrue(); // RESERVED is reversible
    }
}