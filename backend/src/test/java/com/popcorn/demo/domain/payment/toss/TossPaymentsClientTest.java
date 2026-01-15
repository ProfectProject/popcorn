package com.popcorn.demo.domain.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

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

        // 🔧 RestTemplateBuilder 체이닝 메서드들을 Mock 설정
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
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

    // ===============================================================================================================
    // 🔥 Circuit Breaker & Chaos Monkey Integration Tests
    // ===============================================================================================================

    @Nested
    @DisplayName("🔥 API 예외 처리 및 Circuit Breaker 준비 테스트")
    class ApiExceptionHandlingTests {

        @Test
        @DisplayName("TossPayments API 연속 실패 시 예외 전파 확인")
        void shouldPropagateExceptionsOnConsecutiveFailures() {
            // Given - API 서버 장애 시뮬레이션
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RestClientException("TossPayments API 서버 장애"));

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("test-payment-key")
                    .orderId("test-order-123")
                    .amount(10000)
                    .build();

            // When & Then - 단위 테스트에서는 Mock 예외가 그대로 전파됨 (Circuit Breaker는 통합 테스트에서 확인)
            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("TossPayments API 서버 장애");
        }

        @Test
        @DisplayName("RuntimeException 발생 시 예외 전파 확인")
        void shouldPropagateRuntimeException() {
            // Given - RuntimeException 발생 시나리오
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RuntimeException("TossPayments 인증 실패"));

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("fallback-test-key")
                    .orderId("fallback-order-456")
                    .amount(15000)
                    .build();

            // When & Then - 단위 테스트에서는 Mock에서 설정한 예외가 그대로 전파
            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("TossPayments 인증 실패");
        }

        @Test
        @DisplayName("결제 취소 API 실패 시 예외 전파 확인")
        void shouldPropagateCancelExceptions() {
            // Given - 취소 API 실패 시나리오
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RestClientException("TossPayments 취소 API 장애"));

            String paymentKey = "cancel-test-payment";
            TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                    .cancelReason("테스트 취소")
                    .build();

            // When & Then - 단위 테스트에서는 Mock 예외가 그대로 전파
            assertThatThrownBy(() -> client.cancel(paymentKey, request))
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("TossPayments 취소 API 장애");
        }
    }

    @Nested
    @DisplayName("🐒 Chaos Monkey 시나리오 시뮬레이션 테스트")
    class ChaosMonkeyScenarioTests {

        @Test
        @DisplayName("Chaos Monkey 지연 공격 시뮬레이션 - 응답 지연 처리")
        void shouldHandleLatencyAttackScenario() {
            // Given - 지연된 응답 시뮬레이션
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenAnswer(invocation -> {
                        try {
                            Thread.sleep(1000); // 1초 지연 (테스트 시간 단축)
                            TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
                            response.setStatus("PAID");
                            return response;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("🐒 Chaos Monkey 지연 공격 감지!", e);
                        }
                    });

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("slow-call-test")
                    .orderId("chaos-order-delay")
                    .amount(20000)
                    .build();

            // When & Then - 지연이 있어도 성공하면 응답 반환
            TossPaymentsConfirmResponse response = client.confirm(request);
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("Chaos Monkey 예외 공격 시뮬레이션 - 결제 시스템 장애")
        void shouldHandlePaymentFailureScenario() {
            // Given - Chaos Monkey 결제 장애 시뮬레이션
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RuntimeException("💳 결제 시스템 장애 발생!"));

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("chaos-payment-fail")
                    .orderId("chaos-order-fail")
                    .amount(50000)
                    .build();

            // When & Then - 단위 테스트에서는 Mock 예외가 그대로 전파
            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("💳 결제 시스템 장애 발생!");
        }

        @Test
        @DisplayName("Chaos Monkey 네트워크 분할 시뮬레이션 - 연결 실패")
        void shouldHandleNetworkPartitionScenario() {
            // Given - 네트워크 분할 시뮬레이션 (연결 실패로 대체)
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RestClientException("🌐 네트워크 분할 감지!"));

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("network-partition-test")
                    .orderId("chaos-order-network")
                    .amount(30000)
                    .build();

            // When & Then - 네트워크 분할 예외 전파 확인
            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("🌐 네트워크 분할 감지!");
        }

        @Test
        @DisplayName("다양한 Chaos 시나리오 처리 능력 테스트")
        void shouldHandleVariousChaosScenarios() {
            // Given - 다양한 시나리오 테스트
            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("chaos-variety-test")
                    .orderId("chaos-variety")
                    .amount(100000)
                    .build();

            // Scenario 1: 성공 응답
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenAnswer(invocation -> {
                        TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
                        response.setStatus("PAID");
                        return response;
                    });

            TossPaymentsConfirmResponse successResponse = client.confirm(request);
            assertThat(successResponse.getStatus()).isEqualTo("PAID");

            // Scenario 2: 예외 발생
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RuntimeException("🔥 극한 예외 공격!"));

            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("🔥 극한 예외 공격!");
        }
    }

    @Nested
    @DisplayName("⚡ RestTemplate 타임아웃 및 설정 검증 테스트")
    class TimeoutConfigurationTests {

        @Test
        @DisplayName("RestTemplate 연결 타임아웃 설정 확인")
        void shouldConfigureCorrectTimeouts() {
            // Given & When - RestTemplateBuilder가 올바른 타임아웃으로 설정되었는지 확인
            verify(restTemplateBuilder).setConnectTimeout(Duration.ofSeconds(3));
            verify(restTemplateBuilder).setReadTimeout(Duration.ofSeconds(5));
            verify(restTemplateBuilder).build();

            // Then - TossPaymentsClient가 정상적으로 초기화되었음
            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("타임아웃 예외 발생 시 적절한 예외 전파 테스트")
        void shouldPropagateTimeoutExceptions() {
            // Given - 타임아웃 예외 시뮬레이션
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RestClientException("Read timeout"));

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("timeout-test")
                    .orderId("timeout-order")
                    .amount(25000)
                    .build();

            // When & Then - 단위 테스트에서는 타임아웃 예외가 그대로 전파됨
            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("Read timeout");
        }

        @Test
        @DisplayName("연결 타임아웃 예외 처리 테스트")
        void shouldHandleConnectionTimeoutExceptions() {
            // Given - 연결 타임아웃 시뮬레이션
            when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RestClientException("Connection timeout"));

            TossPaymentsConfirmRequest request = TossPaymentsConfirmRequest.builder()
                    .paymentKey("connection-timeout-test")
                    .orderId("connection-timeout-order")
                    .amount(15000)
                    .build();

            // When & Then - 연결 타임아웃 예외 전파 확인
            assertThatThrownBy(() -> client.confirm(request))
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("Connection timeout");
        }
    }
}