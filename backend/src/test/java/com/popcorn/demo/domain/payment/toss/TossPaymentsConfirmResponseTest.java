package com.popcorn.demo.domain.payment.toss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("✅ TossPayments 결제 승인 응답 테스트")
class TossPaymentsConfirmResponseTest {

    @Test
    @DisplayName("결제 승인 응답 객체 생성 및 속성 설정 테스트")
    void shouldCreateResponseAndSetProperties() {
        // Given
        TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();

        // When
        response.setPaymentKey("test-payment-key-123");
        response.setOrderId("test-order-456");
        response.setTotalAmount(25000);
        response.setStatus("PAID");
        response.setMethod("카드");
        response.setRequestedAt("2026-01-16T02:30:00+09:00");
        response.setApprovedAt("2026-01-16T02:30:05+09:00");

        // Then
        assertThat(response.getPaymentKey()).isEqualTo("test-payment-key-123");
        assertThat(response.getOrderId()).isEqualTo("test-order-456");
        assertThat(response.getTotalAmount()).isEqualTo(25000);
        assertThat(response.getStatus()).isEqualTo("PAID");
        assertThat(response.getMethod()).isEqualTo("카드");
        assertThat(response.getRequestedAt()).isEqualTo("2026-01-16T02:30:00+09:00");
        assertThat(response.getApprovedAt()).isEqualTo("2026-01-16T02:30:05+09:00");
    }

    @Test
    @DisplayName("결제 승인 응답 객체 기본 생성자 테스트")
    void shouldCreateEmptyResponse() {
        // Given & When
        TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();

        // Then - 모든 속성이 기본값으로 초기화됨
        assertThat(response.getPaymentKey()).isNull();
        assertThat(response.getOrderId()).isNull();
        assertThat(response.getTotalAmount()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getMethod()).isNull();
        assertThat(response.getRequestedAt()).isNull();
        assertThat(response.getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("결제 상태별 응답 객체 테스트")
    void shouldHandleDifferentPaymentStatuses() {
        // Given & When - PAID 상태
        TossPaymentsConfirmResponse paidResponse = new TossPaymentsConfirmResponse();
        paidResponse.setStatus("PAID");

        // When - FAILED 상태
        TossPaymentsConfirmResponse failedResponse = new TossPaymentsConfirmResponse();
        failedResponse.setStatus("FAILED");

        // When - CANCELED 상태
        TossPaymentsConfirmResponse canceledResponse = new TossPaymentsConfirmResponse();
        canceledResponse.setStatus("CANCELED");

        // Then
        assertThat(paidResponse.getStatus()).isEqualTo("PAID");
        assertThat(failedResponse.getStatus()).isEqualTo("FAILED");
        assertThat(canceledResponse.getStatus()).isEqualTo("CANCELED");
    }
}