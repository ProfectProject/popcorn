package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.dto.BaseResponse;

class TossWebhookControllerTest {

    private TossWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new TossWebhookController();
    }

    @Test
    void handleTossWebhook_WithValidPayload_ReturnsOk() {
        // given
        Map<String, Object> payload = Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "data", Map.of(
                        "paymentKey", "payment-key-123",
                        "orderId", "order-456",
                        "status", "DONE",
                        "totalAmount", 50000,
                        "method", "카드"
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(payload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithEmptyPayload_ReturnsOk() {
        // given
        Map<String, Object> emptyPayload = Map.of();

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(emptyPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithComplexPayload_ReturnsOk() {
        // given
        Map<String, Object> complexPayload = Map.of(
                "eventType", "PAYMENT_STATUS_CHANGED",
                "data", Map.of(
                        "paymentKey", "payment-key-789",
                        "orderId", "order-012",
                        "status", "CANCELED",
                        "totalAmount", 75000,
                        "method", "계좌이체"
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(complexPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithNullPayload_ReturnsOk() {
        // given
        Map<String, Object> nullPayload = null;

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(nullPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithSpecialCharacters_ReturnsOk() {
        // given
        Map<String, Object> specialPayload = Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "data", Map.of(
                        "customerName", "김철수 & 이영희",
                        "orderName", "팝콘 특가상품 <50%할인>",
                        "description", "Test payment with special chars: @#$%^&*()",
                        "unicode", "유니코드 테스트 🎬 🍿"
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(specialPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_AlwaysReturnsOk() {
        // given
        Map<String, Object> anyPayload = Map.of("test", "value");

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(anyPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
        assertThat(response.getBody().getData()).isNotNull();
    }

    @Test
    void handleTossWebhook_WithLargePayload_ReturnsOk() {
        // given
        Map<String, Object> largePayload = Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "createdAt", "2024-01-01T10:00:00+09:00",
                "data", Map.of(
                        "paymentKey", "payment-key-large-data-test-" + "x".repeat(100),
                        "orderId", "order-" + "y".repeat(50),
                        "status", "DONE",
                        "totalAmount", 999999,
                        "method", "카드",
                        "customerName", "김철수김철수김철수김철수",
                        "orderName", "팝콘 대용량 주문".repeat(10)
                ),
                "metadata", Map.of(
                        "version", "1.0",
                        "source", "toss-payments"
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(largePayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithNestedObjects_ReturnsOk() {
        // given
        Map<String, Object> nestedPayload = Map.of(
                "webhook", Map.of(
                        "id", "webhook-123",
                        "event", Map.of(
                                "type", "payment.confirmed",
                                "data", Map.of(
                                        "payment", Map.of(
                                                "id", "payment-nested-test",
                                                "amount", 50000
                                        )
                                )
                        )
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(nestedPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithVariousDataTypes_ReturnsOk() {
        // given
        Map<String, Object> mixedPayload = Map.of(
                "stringField", "test",
                "numberField", 123,
                "booleanField", true,
                "arrayField", List.of("item1", "item2", "item3"),
                "objectField", Map.of(
                        "nested", "value",
                        "number", 456
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(mixedPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithPaymentCancellation_ReturnsOk() {
        // given
        Map<String, Object> cancellationPayload = Map.of(
                "eventType", "PAYMENT_CANCELLED",
                "data", Map.of(
                        "paymentKey", "payment-key-cancel",
                        "orderId", "order-cancel-123",
                        "status", "CANCELLED",
                        "cancels", List.of(
                                Map.of(
                                        "cancelAmount", 30000,
                                        "cancelReason", "고객 취소 요청",
                                        "canceledAt", "2024-01-01T15:00:00+09:00"
                                )
                        )
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(cancellationPayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }

    @Test
    void handleTossWebhook_WithPaymentFailure_ReturnsOk() {
        // given
        Map<String, Object> failurePayload = Map.of(
                "eventType", "PAYMENT_FAILED",
                "data", Map.of(
                        "paymentKey", "payment-key-failed",
                        "orderId", "order-failed-456",
                        "status", "FAILED",
                        "failure", Map.of(
                                "code", "INSUFFICIENT_FUNDS",
                                "message", "잔액이 부족합니다"
                        )
                )
        );

        // when
        ResponseEntity<BaseResponse<String>> response = controller.handleTossWebhook(failurePayload);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("ok");
    }
}