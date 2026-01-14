package com.popcorn.demo.domain.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderItem;

class PaymentSuccessEventTest {

    @Test
    void createEventWithAllParametersUsingBuilder() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-12345";
        UUID paymentId = UUID.randomUUID();
        String orderType = "RESERVATION";
        Integer totalAmount = 50000;
        Long userId = 1001L;
        List<OrderItem> orderItems = List.of(
                OrderItem.builder().id(UUID.randomUUID()).qty(1).build(),
                OrderItem.builder().id(UUID.randomUUID()).qty(2).build()
        );
        LocalDateTime paidAt = LocalDateTime.now();
        String paymentMethod = "TOSS";
        String paymentKey = "payment-key-123";

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .orderType(orderType)
                .totalAmount(totalAmount)
                .userId(userId)
                .orderItems(orderItems)
                .paidAt(paidAt)
                .paymentMethod(paymentMethod)
                .paymentKey(paymentKey)
                .build();

        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getOrderNo()).isEqualTo(orderNo);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderType()).isEqualTo(orderType);
        assertThat(event.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getOrderItems()).hasSize(2);
        assertThat(event.getPaidAt()).isEqualTo(paidAt);
        assertThat(event.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(event.getPaymentKey()).isEqualTo(paymentKey);
    }

    @Test
    void createEventWithMinimalParameters() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-67890";
        UUID paymentId = UUID.randomUUID();
        Long userId = 2002L;

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .userId(userId)
                .build();

        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getOrderNo()).isEqualTo(orderNo);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getOrderType()).isNull();
        assertThat(event.getTotalAmount()).isNull();
        assertThat(event.getOrderItems()).isNull();
    }

    @Test
    void createEventWithPurchaseOrderType() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-PURCHASE-1";
        UUID paymentId = UUID.randomUUID();
        String orderType = "PURCHASE";
        Integer totalAmount = 30000;
        Long userId = 1003L;
        String paymentMethod = "CARD";

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .orderType(orderType)
                .totalAmount(totalAmount)
                .userId(userId)
                .paymentMethod(paymentMethod)
                .build();

        assertThat(event.getOrderType()).isEqualTo("PURCHASE");
        assertThat(event.getPaymentMethod()).isEqualTo("CARD");
    }

    @Test
    void createEventWithEmptyOrderItems() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-EMPTY-1";
        UUID paymentId = UUID.randomUUID();
        String orderType = "RESERVATION";
        Integer totalAmount = 0;
        Long userId = 1004L;
        List<OrderItem> orderItems = List.of();

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .orderType(orderType)
                .totalAmount(totalAmount)
                .userId(userId)
                .orderItems(orderItems)
                .build();

        assertThat(event.getOrderItems()).isEmpty();
        assertThat(event.getTotalAmount()).isEqualTo(0);
    }

    @Test
    void createEventWithHighAmount() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-HIGH-1";
        UUID paymentId = UUID.randomUUID();
        String orderType = "PURCHASE";
        Integer totalAmount = 1000000;
        Long userId = 1005L;

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .orderType(orderType)
                .totalAmount(totalAmount)
                .userId(userId)
                .build();

        assertThat(event.getTotalAmount()).isEqualTo(1000000);
    }

    @Test
    void createEventWithDifferentPaymentKeys() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-KEY-TEST";
        UUID paymentId = UUID.randomUUID();
        Long userId = 1006L;

        PaymentSuccessEvent event1 = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .userId(userId)
                .paymentKey("tgen_payment_key_1")
                .build();

        PaymentSuccessEvent event2 = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .userId(userId)
                .paymentKey("card_payment_key_2")
                .build();

        assertThat(event1.getPaymentKey()).isEqualTo("tgen_payment_key_1");
        assertThat(event2.getPaymentKey()).isEqualTo("card_payment_key_2");
    }

    @Test
    void toStringContainsKeyInformation() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-TEST-123";
        UUID paymentId = UUID.randomUUID();
        String orderType = "RESERVATION";
        Integer totalAmount = 75000;
        Long userId = 1007L;

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .orderType(orderType)
                .totalAmount(totalAmount)
                .userId(userId)
                .build();

        String toString = event.toString();

        assertThat(toString).contains("PaymentSuccessEvent");
        assertThat(toString).contains(orderNo);
        assertThat(toString).contains(paymentId.toString());
        assertThat(toString).contains("75000");
        assertThat(toString).contains("1007");
    }

    @Test
    void toStringFormatIsCorrect() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-FORMAT-TEST";
        UUID paymentId = UUID.randomUUID();
        Integer totalAmount = 50000;
        Long userId = 1008L;

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .totalAmount(totalAmount)
                .userId(userId)
                .build();

        String toString = event.toString();
        String expected = String.format("PaymentSuccessEvent(orderNo=%s, paymentId=%s, amount=%d, userId=%d)",
                orderNo, paymentId, totalAmount, userId);

        assertThat(toString).isEqualTo(expected);
    }

    @Test
    void toStringWithNullValues() {
        UUID orderId = UUID.randomUUID();
        String orderNo = "O-NULL-TEST";
        UUID paymentId = UUID.randomUUID();
        Long userId = 1009L;

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .paymentId(paymentId)
                .totalAmount(null)
                .userId(userId)
                .build();

        String toString = event.toString();

        assertThat(toString).contains("PaymentSuccessEvent");
        assertThat(toString).contains(orderNo);
        assertThat(toString).contains(paymentId.toString());
        assertThat(toString).contains("null"); // totalAmount이 null일 때
        assertThat(toString).contains("1009");
    }
}