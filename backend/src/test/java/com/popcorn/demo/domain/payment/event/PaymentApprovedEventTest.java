package com.popcorn.demo.domain.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;

class PaymentApprovedEventTest {

    @Test
    void createEventWithAllParameters() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 50000;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1001L;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, approvedAt
        );

        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getMethod()).isEqualTo(method);
        assertThat(event.getAmount()).isEqualTo(amount);
        assertThat(event.getOrderType()).isEqualTo("RESERVATION");
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getApprovedAt()).isEqualTo(approvedAt);
    }

    @Test
    void createEventWithNullOrderType() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.TRANSFER;
        Integer amount = 30000;
        Long customerId = 1002L;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                null, customerId, approvedAt
        );

        assertThat(event.getOrderType()).isEqualTo("PURCHASE");
    }

    @Test
    void createEventWithPurchaseOrderType() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.EASY_PAY;
        Integer amount = 75000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1003L;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, approvedAt
        );

        assertThat(event.getOrderType()).isEqualTo("PURCHASE");
    }

    @Test
    void createEventWithZeroAmount() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 0;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1004L;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, approvedAt
        );

        assertThat(event.getAmount()).isEqualTo(0);
    }

    @Test
    void createEventWithHighAmount() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 500000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, approvedAt
        );

        assertThat(event.getAmount()).isEqualTo(500000);
    }

    @Test
    void createEventWithDifferentPaymentMethods() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Integer amount = 40000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent cardEvent = new PaymentApprovedEvent(
                source, paymentId, orderId, PaymentMethod.CARD, amount,
                orderType, customerId, approvedAt
        );

        PaymentApprovedEvent transferEvent = new PaymentApprovedEvent(
                source, paymentId, orderId, PaymentMethod.TRANSFER, amount,
                orderType, customerId, approvedAt
        );

        PaymentApprovedEvent easyPayEvent = new PaymentApprovedEvent(
                source, paymentId, orderId, PaymentMethod.EASY_PAY, amount,
                orderType, customerId, approvedAt
        );

        assertThat(cardEvent.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(transferEvent.getMethod()).isEqualTo(PaymentMethod.TRANSFER);
        assertThat(easyPayEvent.getMethod()).isEqualTo(PaymentMethod.EASY_PAY);
    }

    @Test
    void createEventWithDifferentCustomerIds() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 60000;
        OrderType orderType = OrderType.RESERVATION;
        LocalDateTime approvedAt = LocalDateTime.now();

        PaymentApprovedEvent event1 = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                orderType, 1001L, approvedAt
        );

        PaymentApprovedEvent event2 = new PaymentApprovedEvent(
                source, paymentId, orderId, method, amount,
                orderType, 2002L, approvedAt
        );

        assertThat(event1.getCustomerId()).isEqualTo(1001L);
        assertThat(event2.getCustomerId()).isEqualTo(2002L);
    }
}