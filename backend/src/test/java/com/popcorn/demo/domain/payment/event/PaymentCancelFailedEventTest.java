package com.popcorn.demo.domain.payment.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentCancelFailedEvent 테스트")
class PaymentCancelFailedEventTest {

    @Test
    @DisplayName("PaymentCancelFailedEvent 생성 및 getter 메서드 테스트")
    void createPaymentCancelFailedEventTest() {
        // Given
        Object source = new Object();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String paymentKey = "test_payment_key";
        String cancelReason = "사용자 취소";
        String failureReason = "네트워크 오류";
        Integer amount = 10000;
        LocalDateTime failedAt = LocalDateTime.now();
        int attemptCount = 3;

        // When
        PaymentCancelFailedEvent event = new PaymentCancelFailedEvent(
            source, orderId, paymentId, paymentKey, cancelReason,
            failureReason, amount, failedAt, attemptCount
        );

        // Then
        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(event.getCancelReason()).isEqualTo(cancelReason);
        assertThat(event.getFailureReason()).isEqualTo(failureReason);
        assertThat(event.getAmount()).isEqualTo(amount);
        assertThat(event.getFailedAt()).isEqualTo(failedAt);
        assertThat(event.getAttemptCount()).isEqualTo(attemptCount);
    }

    @Test
    @DisplayName("PaymentCancelFailedEvent null 값들과 함께 생성 테스트")
    void createPaymentCancelFailedEventWithNullValuesTest() {
        // Given
        Object source = new Object();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String paymentKey = null;
        String cancelReason = null;
        String failureReason = null;
        Integer amount = null;
        LocalDateTime failedAt = null;
        int attemptCount = 0;

        // When
        PaymentCancelFailedEvent event = new PaymentCancelFailedEvent(
            source, orderId, paymentId, paymentKey, cancelReason,
            failureReason, amount, failedAt, attemptCount
        );

        // Then
        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getPaymentKey()).isNull();
        assertThat(event.getCancelReason()).isNull();
        assertThat(event.getFailureReason()).isNull();
        assertThat(event.getAmount()).isNull();
        assertThat(event.getFailedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("PaymentCancelFailedEvent는 ApplicationEvent를 상속받는다")
    void paymentCancelFailedEventExtendsApplicationEvent() {
        // Given
        Object source = new Object();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        // When
        PaymentCancelFailedEvent event = new PaymentCancelFailedEvent(
            source, orderId, paymentId, "key", "reason", "failure", 1000, LocalDateTime.now(), 1
        );

        // Then
        assertThat(event).isInstanceOf(org.springframework.context.ApplicationEvent.class);
        assertThat(event.getTimestamp()).isGreaterThan(0);
    }
}