package com.popcorn.demo.domain.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;

class PaymentFailedEventTest {

    @Test
    void createEventWithAllParameters() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 50000;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1001L;
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent event = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, failedAt
        );

        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getMethod()).isEqualTo(method);
        assertThat(event.getAmount()).isEqualTo(amount);
        assertThat(event.getOrderType()).isEqualTo("RESERVATION");
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getFailedAt()).isEqualTo(failedAt);
    }

    @Test
    void createEventWithNullOrderType() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.TRANSFER;
        Integer amount = 30000;
        Long customerId = 1002L;
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent event = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                null, customerId, failedAt
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
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent event = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, failedAt
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
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent event = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, failedAt
        );

        assertThat(event.getAmount()).isEqualTo(0);
    }

    @Test
    void createEventWithHighAmount() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 1000000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent event = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, failedAt
        );

        assertThat(event.getAmount()).isEqualTo(1000000);
    }

    @Test
    void createEventWithDifferentPaymentMethods() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Integer amount = 40000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent cardEvent = new PaymentFailedEvent(
                source, paymentId, orderId, PaymentMethod.CARD, amount,
                orderType, customerId, failedAt
        );

        PaymentFailedEvent transferEvent = new PaymentFailedEvent(
                source, paymentId, orderId, PaymentMethod.TRANSFER, amount,
                orderType, customerId, failedAt
        );

        PaymentFailedEvent easyPayEvent = new PaymentFailedEvent(
                source, paymentId, orderId, PaymentMethod.EASY_PAY, amount,
                orderType, customerId, failedAt
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
        LocalDateTime failedAt = LocalDateTime.now();

        PaymentFailedEvent event1 = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, 1001L, failedAt
        );

        PaymentFailedEvent event2 = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, 2002L, failedAt
        );

        assertThat(event1.getCustomerId()).isEqualTo(1001L);
        assertThat(event2.getCustomerId()).isEqualTo(2002L);
    }

    @Test
    void createEventWithPastFailureTime() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 25000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1007L;
        LocalDateTime failedAt = LocalDateTime.now().minusHours(1);

        PaymentFailedEvent event = new PaymentFailedEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, failedAt
        );

        assertThat(event.getFailedAt()).isBefore(LocalDateTime.now());
    }
}