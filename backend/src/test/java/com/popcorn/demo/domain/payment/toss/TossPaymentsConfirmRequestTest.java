package com.popcorn.demo.domain.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TossPaymentsConfirmRequestTest {

    @Test
    void requestCanBeBuiltWithAllFields() {
        // Given & When
        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("payment-key-123")
                .orderId("order-456")
                .amount(50000)
                .build();

        // Then
        assertThat(request.getPaymentKey()).isEqualTo("payment-key-123");
        assertThat(request.getOrderId()).isEqualTo("order-456");
        assertThat(request.getAmount()).isEqualTo(50000);
    }

    @Test
    void requestCanBeBuiltWithPartialFields() {
        // Given & When
        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("partial-payment-key")
                .build();

        // Then
        assertThat(request.getPaymentKey()).isEqualTo("partial-payment-key");
        assertThat(request.getOrderId()).isNull();
        assertThat(request.getAmount()).isNull();
    }

    @Test
    void requestCanBeCreatedWithNoArgsConstructor() {
        // Given & When
        TossPaymentsConfirmRequest request = new TossPaymentsConfirmRequest();

        // Then
        assertThat(request.getPaymentKey()).isNull();
        assertThat(request.getOrderId()).isNull();
        assertThat(request.getAmount()).isNull();
    }

    @Test
    void requestCanBeCreatedWithAllArgsConstructor() {
        // Given & When
        TossPaymentsConfirmRequest request = new TossPaymentsConfirmRequest(
                "all-args-payment-key",
                "all-args-order-id",
                25000
        );

        // Then
        assertThat(request.getPaymentKey()).isEqualTo("all-args-payment-key");
        assertThat(request.getOrderId()).isEqualTo("all-args-order-id");
        assertThat(request.getAmount()).isEqualTo(25000);
    }

    @Test
    void requestHandlesNullValues() {
        // Given & When
        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey(null)
                .orderId(null)
                .amount(null)
                .build();

        // Then
        assertThat(request.getPaymentKey()).isNull();
        assertThat(request.getOrderId()).isNull();
        assertThat(request.getAmount()).isNull();
    }

    @Test
    void requestHandlesZeroAmount() {
        // Given & When
        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("zero-amount-key")
                .orderId("zero-amount-order")
                .amount(0)
                .build();

        // Then
        assertThat(request.getPaymentKey()).isEqualTo("zero-amount-key");
        assertThat(request.getOrderId()).isEqualTo("zero-amount-order");
        assertThat(request.getAmount()).isEqualTo(0);
    }
}