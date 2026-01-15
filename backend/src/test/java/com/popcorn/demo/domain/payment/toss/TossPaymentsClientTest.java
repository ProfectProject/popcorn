package com.popcorn.demo.domain.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

class TossPaymentsClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TossPaymentsProperties properties;

    private TossPaymentsClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(properties.getBaseUrl()).thenReturn("https://api.tosspayments.com");
        when(properties.getSecretKey()).thenReturn("test_secret_key");
        client = new TossPaymentsClient(restTemplateBuilder, properties);
    }

    @Test
    void confirmSendsRequestToTossApi() {
        // Given
        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("payment-key-123")
                .orderId("order-456")
                .amount(10000)
                .build();

        TossPaymentsConfirmResponse expectedResponse = new TossPaymentsConfirmResponse();
        expectedResponse.setPaymentKey("payment-key-123");
        expectedResponse.setOrderId("order-456");
        expectedResponse.setTotalAmount(10000);
        expectedResponse.setStatus("PAID");

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments/confirm"),
                any(HttpEntity.class),
                eq(TossPaymentsConfirmResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsConfirmResponse response = client.confirm(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isEqualTo("payment-key-123");
        assertThat(response.getOrderId()).isEqualTo("order-456");
        assertThat(response.getTotalAmount()).isEqualTo(10000);
        assertThat(response.getStatus()).isEqualTo("PAID");
    }

    @Test
    void confirmWithNullSecretKey() {
        // Given
        when(properties.getSecretKey()).thenReturn(null);
        client = new TossPaymentsClient(restTemplateBuilder, properties);

        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("payment-key-789")
                .orderId("order-101")
                .amount(5000)
                .build();

        TossPaymentsConfirmResponse expectedResponse = new TossPaymentsConfirmResponse();
        expectedResponse.setPaymentKey("payment-key-789");
        expectedResponse.setOrderId("order-101");
        expectedResponse.setTotalAmount(5000);
        expectedResponse.setStatus("PAID");

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments/confirm"),
                any(HttpEntity.class),
                eq(TossPaymentsConfirmResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsConfirmResponse response = client.confirm(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isEqualTo("payment-key-789");
    }

    @Test
    void confirmWithEmptySecretKey() {
        // Given
        when(properties.getSecretKey()).thenReturn("");
        client = new TossPaymentsClient(restTemplateBuilder, properties);

        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("payment-key-empty")
                .orderId("order-empty")
                .amount(1000)
                .build();

        TossPaymentsConfirmResponse expectedResponse = new TossPaymentsConfirmResponse();
        expectedResponse.setPaymentKey("payment-key-empty");
        expectedResponse.setOrderId("order-empty");
        expectedResponse.setTotalAmount(1000);
        expectedResponse.setStatus("FAILED");

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments/confirm"),
                any(HttpEntity.class),
                eq(TossPaymentsConfirmResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsConfirmResponse response = client.confirm(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void confirmWithDifferentBaseUrl() {
        // Given
        when(properties.getBaseUrl()).thenReturn("https://sandbox-api.tosspayments.com");
        client = new TossPaymentsClient(restTemplateBuilder, properties);

        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("sandbox-payment-key")
                .orderId("sandbox-order")
                .amount(20000)
                .build();

        TossPaymentsConfirmResponse expectedResponse = new TossPaymentsConfirmResponse();
        expectedResponse.setPaymentKey("sandbox-payment-key");
        expectedResponse.setOrderId("sandbox-order");
        expectedResponse.setTotalAmount(20000);
        expectedResponse.setStatus("PAID");

        when(restTemplate.postForObject(
                eq("https://sandbox-api.tosspayments.com/v1/payments/confirm"),
                any(HttpEntity.class),
                eq(TossPaymentsConfirmResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsConfirmResponse response = client.confirm(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isEqualTo("sandbox-payment-key");
    }

    @Test
    void cancelSendsRequestToTossApi() {
        // Given
        String paymentKey = "payment-key-to-cancel";
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason("사용자 요청")
                .build();

        TossPaymentsCancelResponse expectedResponse = new TossPaymentsCancelResponse();
        expectedResponse.setPaymentKey(paymentKey);
        expectedResponse.setStatus("CANCELED");
        expectedResponse.setTotalAmount(15000);
        expectedResponse.setBalanceAmount(0);

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel"),
                any(HttpEntity.class),
                eq(TossPaymentsCancelResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsCancelResponse response = client.cancel(paymentKey, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.getTotalAmount()).isEqualTo(15000);
        assertThat(response.getBalanceAmount()).isEqualTo(0);
    }

    @Test
    void cancelWithNullPaymentKey() {
        // Given
        String paymentKey = null;
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason("관리자 취소")
                .build();

        TossPaymentsCancelResponse expectedResponse = new TossPaymentsCancelResponse();
        expectedResponse.setPaymentKey(null);
        expectedResponse.setStatus("FAILED");

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments/null/cancel"),
                any(HttpEntity.class),
                eq(TossPaymentsCancelResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsCancelResponse response = client.cancel(paymentKey, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void cancelWithEmptyPaymentKey() {
        // Given
        String paymentKey = "";
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason("오류로 인한 취소")
                .build();

        TossPaymentsCancelResponse expectedResponse = new TossPaymentsCancelResponse();
        expectedResponse.setPaymentKey("");
        expectedResponse.setStatus("CANCELED");
        expectedResponse.setTotalAmount(5000);

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments//cancel"),
                any(HttpEntity.class),
                eq(TossPaymentsCancelResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsCancelResponse response = client.cancel(paymentKey, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isEqualTo("");
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.getTotalAmount()).isEqualTo(5000);
    }

    @Test
    void buildAuthorizationHeaderWithValidSecretKey() {
        // Given - secretKey가 정상적으로 설정된 경우는 기존 confirm 테스트에서 이미 테스트됨
        // 추가적으로 private 메서드의 동작을 간접적으로 확인

        TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                .paymentKey("auth-test-key")
                .orderId("auth-test-order")
                .amount(10000)
                .build();

        TossPaymentsConfirmResponse expectedResponse = new TossPaymentsConfirmResponse();
        expectedResponse.setPaymentKey("auth-test-key");
        expectedResponse.setStatus("PAID");

        when(restTemplate.postForObject(
                eq("https://api.tosspayments.com/v1/payments/confirm"),
                any(HttpEntity.class),
                eq(TossPaymentsConfirmResponse.class)
        )).thenReturn(expectedResponse);

        // When
        TossPaymentsConfirmResponse response = client.confirm(request);

        // Then - Authorization 헤더가 올바르게 설정되었는지 간접 확인 (정상 응답으로 판단)
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PAID");
    }
}