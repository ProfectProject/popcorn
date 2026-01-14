package com.popcorn.demo.domain.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;

class PaymentCancelledEventTest {

    @Test
    void createEventWithAllParameters() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 50000;
        OrderType orderType = OrderType.RESERVATION;
        Long customerId = 1001L;
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, cancelledAt
        );

        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getMethod()).isEqualTo(method);
        assertThat(event.getAmount()).isEqualTo(amount);
        assertThat(event.getOrderType()).isEqualTo("RESERVATION");
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getCancelledAt()).isEqualTo(cancelledAt);
    }

    @Test
    void createEventWithNullOrderType() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.TRANSFER;
        Integer amount = 30000;
        Long customerId = 1002L;
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                null, customerId, cancelledAt
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
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, cancelledAt
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
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, cancelledAt
        );

        assertThat(event.getAmount()).isEqualTo(0);
    }

    @Test
    void createEventWithHighAmount() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 2000000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, cancelledAt
        );

        assertThat(event.getAmount()).isEqualTo(2000000);
    }

    @Test
    void createEventWithDifferentPaymentMethods() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Integer amount = 40000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1005L;
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent cardEvent = new PaymentCancelledEvent(
                source, paymentId, orderId, PaymentMethod.CARD, amount,
                orderType, customerId, cancelledAt
        );

        PaymentCancelledEvent transferEvent = new PaymentCancelledEvent(
                source, paymentId, orderId, PaymentMethod.TRANSFER, amount,
                orderType, customerId, cancelledAt
        );

        PaymentCancelledEvent easyPayEvent = new PaymentCancelledEvent(
                source, paymentId, orderId, PaymentMethod.EASY_PAY, amount,
                orderType, customerId, cancelledAt
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
        LocalDateTime cancelledAt = LocalDateTime.now();

        PaymentCancelledEvent event1 = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, 1001L, cancelledAt
        );

        PaymentCancelledEvent event2 = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, 2002L, cancelledAt
        );

        assertThat(event1.getCustomerId()).isEqualTo(1001L);
        assertThat(event2.getCustomerId()).isEqualTo(2002L);
    }

    @Test
    void createEventWithPastCancellationTime() {
        Object source = this;
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentMethod method = PaymentMethod.CARD;
        Integer amount = 35000;
        OrderType orderType = OrderType.PURCHASE;
        Long customerId = 1007L;
        LocalDateTime cancelledAt = LocalDateTime.now().minusMinutes(30);

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                source, paymentId, orderId, method, amount,
                orderType, customerId, cancelledAt
        );

        assertThat(event.getCancelledAt()).isBefore(LocalDateTime.now());
    }
}