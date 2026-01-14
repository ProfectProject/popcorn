package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderPaymentProcessedEventTest {

    @Test
    void createEventWithAllParameters() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 50000, 5000,
                "payment-123", "tx-456", "TOSS"
        );

        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getUserId()).isEqualTo(1001L);
        assertThat(event.getPaymentMethod()).isEqualTo("CARD");
        assertThat(event.getPaymentStatus()).isEqualTo("COMPLETED");
        assertThat(event.getPaidAmount()).isEqualTo(50000);
        assertThat(event.getDiscountAmount()).isEqualTo(5000);
        assertThat(event.getPaymentId()).isEqualTo("payment-123");
        assertThat(event.getTransactionId()).isEqualTo("tx-456");
        assertThat(event.getPaymentProvider()).isEqualTo("TOSS");
        assertThat(event.getPaymentAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("order_payment_processed");
    }

    @Test
    void createEventWithNullParameters() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, null, null, null, null, null, null, null
        );

        assertThat(event.getPaymentMethod()).isNull();
        assertThat(event.getPaymentStatus()).isNull();
        assertThat(event.getPaidAmount()).isEqualTo(0);
        assertThat(event.getDiscountAmount()).isEqualTo(0);
        assertThat(event.getPaymentId()).isNull();
        assertThat(event.getTransactionId()).isNull();
        assertThat(event.getPaymentProvider()).isNull();
    }

    @Test
    void isSuccessfulPaymentWithCompletedStatus() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        assertThat(event.isSuccessfulPayment()).isTrue();
    }

    @Test
    void isSuccessfulPaymentWithSuccessStatus() {
        OrderPaymentProcessedEvent event = createEvent("SUCCESS");
        assertThat(event.isSuccessfulPayment()).isTrue();
    }

    @Test
    void isSuccessfulPaymentWithFailedStatus() {
        OrderPaymentProcessedEvent event = createEvent("FAILED");
        assertThat(event.isSuccessfulPayment()).isFalse();
    }

    @Test
    void isFailedPaymentWithFailedStatus() {
        OrderPaymentProcessedEvent event = createEvent("FAILED");
        assertThat(event.isFailedPayment()).isTrue();
    }

    @Test
    void isFailedPaymentWithCancelledStatus() {
        OrderPaymentProcessedEvent event = createEvent("CANCELLED");
        assertThat(event.isFailedPayment()).isTrue();
    }

    @Test
    void isFailedPaymentWithRejectedStatus() {
        OrderPaymentProcessedEvent event = createEvent("REJECTED");
        assertThat(event.isFailedPayment()).isTrue();
    }

    @Test
    void isFailedPaymentWithSuccessStatus() {
        OrderPaymentProcessedEvent event = createEvent("SUCCESS");
        assertThat(event.isFailedPayment()).isFalse();
    }

    @Test
    void isRefundPaymentWithRefundedStatus() {
        OrderPaymentProcessedEvent event = createEvent("REFUNDED");
        assertThat(event.isRefundPayment()).isTrue();
    }

    @Test
    void isRefundPaymentWithRefundStatus() {
        OrderPaymentProcessedEvent event = createEvent("REFUND");
        assertThat(event.isRefundPayment()).isTrue();
    }

    @Test
    void isRefundPaymentWithCompletedStatus() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        assertThat(event.isRefundPayment()).isFalse();
    }

    @Test
    void hasDiscountWithPositiveAmount() {
        OrderPaymentProcessedEvent event = createEventWithDiscount(5000);
        assertThat(event.hasDiscount()).isTrue();
    }

    @Test
    void hasDiscountWithZeroAmount() {
        OrderPaymentProcessedEvent event = createEventWithDiscount(0);
        assertThat(event.hasDiscount()).isFalse();
    }

    @Test
    void hasDiscountWithNullAmount() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 50000, null,
                "payment-123", "tx-456", "TOSS"
        );
        assertThat(event.hasDiscount()).isFalse();
    }

    @Test
    void getDiscountRateWithDiscount() {
        // 50000원 결제에 5000원 할인 = 약 9.09% 할인
        OrderPaymentProcessedEvent event = createEventWithDiscount(5000);
        double rate = event.getDiscountRate();
        assertThat(rate).isCloseTo(9.09, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void getDiscountRateWithNoDiscount() {
        OrderPaymentProcessedEvent event = createEventWithDiscount(0);
        assertThat(event.getDiscountRate()).isEqualTo(0.0);
    }

    @Test
    void getDiscountRateWithZeroPaidAmount() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 0, 1000,
                "payment-123", "tx-456", "TOSS"
        );
        assertThat(event.getDiscountRate()).isEqualTo(0.0);
    }

    @Test
    void isHighAmountPaymentWithHighAmount() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 150000, 0,
                "payment-123", "tx-456", "TOSS"
        );
        assertThat(event.isHighAmountPayment()).isTrue();
    }

    @Test
    void isHighAmountPaymentWithLowAmount() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        assertThat(event.isHighAmountPayment()).isFalse(); // 50000 < 100000
    }

    @Test
    void isCashPaymentWithCashMethod() {
        OrderPaymentProcessedEvent event = createEventWithMethod("CASH");
        assertThat(event.isCashPayment()).isTrue();
    }

    @Test
    void isCashPaymentWithKoreanCash() {
        OrderPaymentProcessedEvent event = createEventWithMethod("현금");
        assertThat(event.isCashPayment()).isTrue();
    }

    @Test
    void isCashPaymentWithCardMethod() {
        OrderPaymentProcessedEvent event = createEventWithMethod("CARD");
        assertThat(event.isCashPayment()).isFalse();
    }

    @Test
    void isCardPaymentWithCardMethod() {
        OrderPaymentProcessedEvent event = createEventWithMethod("CARD");
        assertThat(event.isCardPayment()).isTrue();
    }

    @Test
    void isCardPaymentWithCreditCardMethod() {
        OrderPaymentProcessedEvent event = createEventWithMethod("CREDIT_CARD");
        assertThat(event.isCardPayment()).isTrue();
    }

    @Test
    void isCardPaymentWithKoreanCard() {
        OrderPaymentProcessedEvent event = createEventWithMethod("신용카드");
        assertThat(event.isCardPayment()).isTrue();
    }

    @Test
    void isCardPaymentWithCashMethod() {
        OrderPaymentProcessedEvent event = createEventWithMethod("CASH");
        assertThat(event.isCardPayment()).isFalse();
    }

    @Test
    void isCardPaymentWithNullMethod() {
        OrderPaymentProcessedEvent event = createEventWithMethod(null);
        assertThat(event.isCardPayment()).isFalse();
    }

    @Test
    void getPaymentRiskHighAmountCash() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CASH", "COMPLETED", 600000, 0,
                "payment-123", "tx-456", "TOSS"
        );
        assertThat(event.getPaymentRisk()).isEqualTo("HIGH");
    }

    @Test
    void getPaymentRiskMediumAmount() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 300000, 0,
                "payment-123", "tx-456", "TOSS"
        );
        assertThat(event.getPaymentRisk()).isEqualTo("MEDIUM");
    }

    @Test
    void getPaymentRiskLowAmount() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        assertThat(event.getPaymentRisk()).isEqualTo("LOW");
    }

    @Test
    void getSettlementPriorityHighForSuccessfulHighAmount() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 150000, 0,
                "payment-123", "tx-456", "TOSS"
        );
        assertThat(event.getSettlementPriority()).isEqualTo("HIGH");
    }

    @Test
    void getSettlementPriorityUrgentForFailed() {
        OrderPaymentProcessedEvent event = createEvent("FAILED");
        assertThat(event.getSettlementPriority()).isEqualTo("URGENT");
    }

    @Test
    void getSettlementPriorityUrgentForRefund() {
        OrderPaymentProcessedEvent event = createEvent("REFUNDED");
        assertThat(event.getSettlementPriority()).isEqualTo("URGENT");
    }

    @Test
    void getSettlementPriorityNormalForSuccessful() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        assertThat(event.getSettlementPriority()).isEqualTo("NORMAL");
    }

    @Test
    void getSettlementPriorityLowForOther() {
        OrderPaymentProcessedEvent event = createEvent("PENDING");
        assertThat(event.getSettlementPriority()).isEqualTo("LOW");
    }

    @Test
    void getEventPayloadContainsAllFields() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        Map<String, Object> payload = event.getEventPayload();

        assertThat(payload).containsKeys(
                "orderId", "userId", "paymentMethod", "paymentStatus",
                "paidAmount", "discountAmount", "paymentId", "transactionId",
                "paymentProvider", "paymentAt", "isSuccessful", "hasDiscount",
                "discountRate", "riskLevel", "settlementPriority"
        );
        assertThat(payload.get("isSuccessful")).isEqualTo(true);
        assertThat(payload.get("riskLevel")).isEqualTo("LOW");
        assertThat(payload.get("settlementPriority")).isEqualTo("NORMAL");
    }

    @Test
    void getPaymentDescriptionWithDiscount() {
        OrderPaymentProcessedEvent event = createEventWithDiscount(5000);
        String description = event.getPaymentDescription();

        assertThat(description).contains("CARD");
        assertThat(description).contains("COMPLETED");
        assertThat(description).contains("50,000원");
        assertThat(description).contains("할인: 5,000원");
        assertThat(description).contains("9.1%");
        assertThat(description).contains("TOSS");
    }

    @Test
    void getPaymentDescriptionWithoutDiscount() {
        OrderPaymentProcessedEvent event = createEventWithDiscount(0);
        String description = event.getPaymentDescription();

        assertThat(description).contains("CARD");
        assertThat(description).contains("COMPLETED");
        assertThat(description).contains("50,000원");
        assertThat(description).doesNotContain("할인");
        assertThat(description).contains("TOSS");
    }

    @Test
    void getPaymentDescriptionWithNullValues() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
                orderId, 1001L, null, null, 30000, 0,
                null, null, null
        );
        String description = event.getPaymentDescription();

        assertThat(description).contains("미상");
        assertThat(description).contains("30,000원");
    }

    @Test
    void toStringContainsKeyInformation() {
        OrderPaymentProcessedEvent event = createEvent("COMPLETED");
        String toString = event.toString();

        assertThat(toString).contains("OrderPaymentProcessedEvent");
        assertThat(toString).contains("CARD");
        assertThat(toString).contains("COMPLETED");
        assertThat(toString).contains("50000");
    }

    // Helper methods
    private OrderPaymentProcessedEvent createEvent(String status) {
        UUID orderId = UUID.randomUUID();
        return new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", status, 50000, 0,
                "payment-123", "tx-456", "TOSS"
        );
    }

    private OrderPaymentProcessedEvent createEventWithDiscount(Integer discountAmount) {
        UUID orderId = UUID.randomUUID();
        return new OrderPaymentProcessedEvent(
                orderId, 1001L, "CARD", "COMPLETED", 50000, discountAmount,
                "payment-123", "tx-456", "TOSS"
        );
    }

    private OrderPaymentProcessedEvent createEventWithMethod(String method) {
        UUID orderId = UUID.randomUUID();
        return new OrderPaymentProcessedEvent(
                orderId, 1001L, method, "COMPLETED", 50000, 0,
                "payment-123", "tx-456", "TOSS"
        );
    }
}