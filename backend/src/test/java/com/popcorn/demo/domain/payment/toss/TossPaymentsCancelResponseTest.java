package com.popcorn.demo.domain.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("TossPaymentsCancelResponse 테스트")
class TossPaymentsCancelResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    @DisplayName("TossPaymentsCancelResponse getter/setter 테스트")
    void tossPaymentsCancelResponseGetterSetterTest() {
        // Given
        TossPaymentsCancelResponse response = new TossPaymentsCancelResponse();

        // When
        response.setPaymentKey("test_payment_key");
        response.setOrderId("test_order_id");
        response.setStatus("CANCELED");
        response.setTotalAmount(10000);
        response.setBalanceAmount(0);
        response.setMethod("카드");
        response.setRequestedAt("2025-01-15T10:00:00");
        response.setApprovedAt("2025-01-15T10:05:00");

        // Then
        assertThat(response.getPaymentKey()).isEqualTo("test_payment_key");
        assertThat(response.getOrderId()).isEqualTo("test_order_id");
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.getTotalAmount()).isEqualTo(10000);
        assertThat(response.getBalanceAmount()).isEqualTo(0);
        assertThat(response.getMethod()).isEqualTo("카드");
        assertThat(response.getRequestedAt()).isEqualTo("2025-01-15T10:00:00");
        assertThat(response.getApprovedAt()).isEqualTo("2025-01-15T10:05:00");
    }

    @Test
    @DisplayName("TossPaymentsCancelResponse 캔슬 목록 설정 테스트")
    void tossPaymentsCancelResponseCancelsTest() {
        // Given
        TossPaymentsCancelResponse response = new TossPaymentsCancelResponse();
        TossPaymentsCancelResponse.CancelDetail cancelDetail = new TossPaymentsCancelResponse.CancelDetail();

        cancelDetail.setCancelAmount(5000);
        cancelDetail.setCancelReason("부분 취소");
        cancelDetail.setCanceledAt("2025-01-15T10:30:00");

        // When
        response.setCancels(List.of(cancelDetail));

        // Then
        assertThat(response.getCancels()).hasSize(1);
        assertThat(response.getCancels().get(0).getCancelAmount()).isEqualTo(5000);
        assertThat(response.getCancels().get(0).getCancelReason()).isEqualTo("부분 취소");
        assertThat(response.getCancels().get(0).getCanceledAt()).isEqualTo("2025-01-15T10:30:00");
    }

    @Test
    @DisplayName("TossPaymentsCancelResponse null 값들과 함께 테스트")
    void tossPaymentsCancelResponseWithNullValuesTest() {
        // Given
        TossPaymentsCancelResponse response = new TossPaymentsCancelResponse();

        // When - null 값들 설정
        response.setPaymentKey(null);
        response.setOrderId(null);
        response.setStatus(null);
        response.setTotalAmount(null);
        response.setBalanceAmount(null);
        response.setMethod(null);
        response.setRequestedAt(null);
        response.setApprovedAt(null);
        response.setCancels(null);

        // Then
        assertThat(response.getPaymentKey()).isNull();
        assertThat(response.getOrderId()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getTotalAmount()).isNull();
        assertThat(response.getBalanceAmount()).isNull();
        assertThat(response.getMethod()).isNull();
        assertThat(response.getRequestedAt()).isNull();
        assertThat(response.getApprovedAt()).isNull();
        assertThat(response.getCancels()).isNull();
    }

    @Test
    @DisplayName("CancelDetail getter/setter 테스트")
    void cancelDetailGetterSetterTest() {
        // Given
        TossPaymentsCancelResponse.CancelDetail cancelDetail = new TossPaymentsCancelResponse.CancelDetail();

        // When
        cancelDetail.setCancelAmount(3000);
        cancelDetail.setCancelReason("고객 변심");
        cancelDetail.setCanceledAt("2025-01-15T11:00:00");

        // Then
        assertThat(cancelDetail.getCancelAmount()).isEqualTo(3000);
        assertThat(cancelDetail.getCancelReason()).isEqualTo("고객 변심");
        assertThat(cancelDetail.getCanceledAt()).isEqualTo("2025-01-15T11:00:00");
    }

    @Test
    @DisplayName("CancelDetail null 값들과 함께 테스트")
    void cancelDetailWithNullValuesTest() {
        // Given
        TossPaymentsCancelResponse.CancelDetail cancelDetail = new TossPaymentsCancelResponse.CancelDetail();

        // When
        cancelDetail.setCancelAmount(null);
        cancelDetail.setCancelReason(null);
        cancelDetail.setCanceledAt(null);

        // Then
        assertThat(cancelDetail.getCancelAmount()).isNull();
        assertThat(cancelDetail.getCancelReason()).isNull();
        assertThat(cancelDetail.getCanceledAt()).isNull();
    }

    @Test
    @DisplayName("JSON 직렬화/역직렬화 테스트 (Jackson과의 호환성)")
    void jsonSerializationDeserializationTest() throws Exception {
        // Given
        TossPaymentsCancelResponse response = new TossPaymentsCancelResponse();
        response.setPaymentKey("test_payment_key");
        response.setOrderId("test_order_id");
        response.setStatus("CANCELED");
        response.setTotalAmount(10000);
        response.setBalanceAmount(0);
        response.setMethod("카드");

        TossPaymentsCancelResponse.CancelDetail cancelDetail = new TossPaymentsCancelResponse.CancelDetail();
        cancelDetail.setCancelAmount(10000);
        cancelDetail.setCancelReason("전체 취소");
        cancelDetail.setCanceledAt("2025-01-15T10:30:00");
        response.setCancels(List.of(cancelDetail));

        // When - JSON으로 직렬화
        String json = objectMapper.writeValueAsString(response);

        // Then - JSON에서 역직렬화
        TossPaymentsCancelResponse deserializedResponse = objectMapper.readValue(json, TossPaymentsCancelResponse.class);

        assertThat(deserializedResponse.getPaymentKey()).isEqualTo("test_payment_key");
        assertThat(deserializedResponse.getOrderId()).isEqualTo("test_order_id");
        assertThat(deserializedResponse.getStatus()).isEqualTo("CANCELED");
        assertThat(deserializedResponse.getTotalAmount()).isEqualTo(10000);
        assertThat(deserializedResponse.getCancels()).hasSize(1);
        assertThat(deserializedResponse.getCancels().get(0).getCancelAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("@JsonIgnoreProperties 어노테이션 동작 테스트")
    void jsonIgnoreUnknownPropertiesTest() throws Exception {
        // Given - 알 수 없는 속성이 포함된 JSON
        String jsonWithUnknownProperties = """
            {
                "paymentKey": "test_payment_key",
                "orderId": "test_order_id",
                "status": "CANCELED",
                "totalAmount": 10000,
                "unknownProperty1": "value1",
                "unknownProperty2": 999,
                "cancels": [
                    {
                        "cancelAmount": 10000,
                        "cancelReason": "전체 취소",
                        "canceledAt": "2025-01-15T10:30:00",
                        "unknownCancelProperty": "ignored"
                    }
                ]
            }
            """;

        // When - 알 수 없는 속성이 있어도 역직렬화가 성공해야 함
        TossPaymentsCancelResponse response = objectMapper.readValue(jsonWithUnknownProperties, TossPaymentsCancelResponse.class);

        // Then
        assertThat(response.getPaymentKey()).isEqualTo("test_payment_key");
        assertThat(response.getOrderId()).isEqualTo("test_order_id");
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.getTotalAmount()).isEqualTo(10000);
        assertThat(response.getCancels()).hasSize(1);
        assertThat(response.getCancels().get(0).getCancelAmount()).isEqualTo(10000);
    }
}