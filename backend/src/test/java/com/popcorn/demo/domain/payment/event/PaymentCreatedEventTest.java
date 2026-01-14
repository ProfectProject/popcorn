package com.popcorn.demo.domain.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;

class PaymentCreatedEventTest {

    @Test
    void createEventWithAllParameters() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.READY;
        Integer amount = 50000;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1001L;
        LocalDateTime createdAt = LocalDateTime.now();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                source, paymentId, orderId, method, status, amount,
                orderType, customerId, createdAt
        );

        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getMethod()).isEqualTo(method);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getAmount()).isEqualTo(amount);
        assertThat(event.getOrderType()).isEqualTo("RESERVATION");
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void createEventWithNullOrderType() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.TRANSFER;
        PaymentStatus status = PaymentStatus.READY;
        Integer amount = 30000;
        Long customerId = 1002L;
        LocalDateTime createdAt = LocalDateTime.now();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                source, paymentId, orderId, method, status, amount,
                null, customerId, createdAt
        );

        assertThat(event.getOrderType()).isEqualTo("PURCHASE");
    }

    @Test
    void createEventWithPurchaseOrderType() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.EASY_PAY;
        PaymentStatus status = PaymentStatus.PAID;
        Integer amount = 75000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1003L;
        LocalDateTime createdAt = LocalDateTime.now();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                source, paymentId, orderId, method, status, amount,
                orderType, customerId, createdAt
        );

        assertThat(event.getOrderType()).isEqualTo("PURCHASE");
    }

    @Test
    void createEventWithZeroAmount() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.READY;
        Integer amount = 0;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1004L;
        LocalDateTime createdAt = LocalDateTime.now();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                source, paymentId, orderId, method, status, amount,
                orderType, customerId, createdAt
        );

        assertThat(event.getAmount()).isZero();
    }

    @Test
    void createEventWithDifferentPaymentMethods() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentStatus status = PaymentStatus.READY;
        Integer amount = 40000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime createdAt = LocalDateTime.now();

        PaymentCreatedEvent cardEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, PaymentMethod.CARD, status, amount,
                orderType, customerId, createdAt
        );

        PaymentCreatedEvent transferEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, PaymentMethod.TRANSFER, status, amount,
                orderType, customerId, createdAt
        );

        PaymentCreatedEvent easyPayEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, PaymentMethod.EASY_PAY, status, amount,
                orderType, customerId, createdAt
        );

        assertThat(cardEvent.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(transferEvent.getMethod()).isEqualTo(PaymentMethod.TRANSFER);
        assertThat(easyPayEvent.getMethod()).isEqualTo(PaymentMethod.EASY_PAY);
    }

    @Test
    void createEventWithDifferentPaymentStatuses() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 60000;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1006L;
        LocalDateTime createdAt = LocalDateTime.now();

        PaymentCreatedEvent readyEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, method, PaymentStatus.READY, amount,
                orderType, customerId, createdAt
        );

        PaymentCreatedEvent paidEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, method, PaymentStatus.PAID, amount,
                orderType, customerId, createdAt
        );

        PaymentCreatedEvent failedEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, method, PaymentStatus.FAILED, amount,
                orderType, customerId, createdAt
        );

        PaymentCreatedEvent cancelledEvent = new PaymentCreatedEvent(
                source, paymentId, orderId, method, PaymentStatus.CANCELLED, amount,
                orderType, customerId, createdAt
        );

        assertThat(readyEvent.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(paidEvent.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(failedEvent.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(cancelledEvent.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }
}