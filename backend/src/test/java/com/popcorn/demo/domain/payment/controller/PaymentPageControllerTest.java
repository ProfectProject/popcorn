package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.payment.service.PaymentTokenService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;

class PaymentPageControllerTest {

    @Mock
    private PaymentTokenService paymentTokenService;

    @Mock
    private TossPaymentsProperties tossPaymentsProperties;

    private PaymentPageController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PaymentPageController(paymentTokenService, tossPaymentsProperties);
    }

    @Test
    void redirectToPaymentPage_WithValidToken_ReturnsRedirect() {
        // given
        String token = "valid-jwt-token";
        String checkoutUrl = "https://checkout.toss.im";

        when(paymentTokenService.isValidToken(token)).thenReturn(true);
        when(tossPaymentsProperties.getCheckoutUrl()).thenReturn(checkoutUrl);

        // when
        ResponseEntity<Void> response = controller.redirectToPaymentPage(token);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString()).contains(checkoutUrl);
        assertThat(response.getHeaders().getLocation().toString()).contains("token=" + token);

        verify(paymentTokenService).isValidToken(token);
        verify(tossPaymentsProperties).getCheckoutUrl();
    }

    @Test
    void redirectToPaymentPage_WithInvalidToken_ReturnsBadRequest() {
        // given
        String invalidToken = "invalid-jwt-token";

        when(paymentTokenService.isValidToken(invalidToken)).thenReturn(false);

        // when
        ResponseEntity<Void> response = controller.redirectToPaymentPage(invalidToken);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getHeaders().getLocation()).isNull();

        verify(paymentTokenService).isValidToken(invalidToken);
    }

    @Test
    void decodePaymentToken_WithValidToken_ReturnsPaymentInfo() {
        // given
        String token = "valid-jwt-token";
        UUID orderId = UUID.randomUUID();
        PaymentTokenService.PaymentTokenInfo expectedInfo =
                PaymentTokenService.PaymentTokenInfo.builder()
                        .orderId(orderId)
                        .amount(50000)
                        .customerKey("customer123")
                        .orderNo("테스트주문001")
                        .successUrl("https://success.url")
                        .failUrl("https://fail.url")
                        .build();

        when(paymentTokenService.parsePaymentToken(token)).thenReturn(expectedInfo);

        // when
        ResponseEntity<BaseResponse<PaymentTokenService.PaymentTokenInfo>> response =
                controller.decodePaymentToken(token);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(expectedInfo);
        assertThat(response.getBody().getData().getOrderId()).isEqualTo(orderId);
        assertThat(response.getBody().getData().getAmount()).isEqualTo(50000);

        verify(paymentTokenService).parsePaymentToken(token);
    }

    @Test
    void decodePaymentToken_WithInvalidToken_ReturnsBadRequest() {
        // given
        String invalidToken = "invalid-jwt-token";

        when(paymentTokenService.parsePaymentToken(invalidToken))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // when
        ResponseEntity<BaseResponse<PaymentTokenService.PaymentTokenInfo>> response =
                controller.decodePaymentToken(invalidToken);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNull();

        verify(paymentTokenService).parsePaymentToken(invalidToken);
    }

    @Test
    void redirectToPaymentPage_WithSpecialCharacters_HandlesCorrectly() {
        // given
        String token = "token-with-special-chars-!@#$%";
        String checkoutUrl = "https://checkout.toss.im/pay";

        when(paymentTokenService.isValidToken(token)).thenReturn(true);
        when(tossPaymentsProperties.getCheckoutUrl()).thenReturn(checkoutUrl);

        // when
        ResponseEntity<Void> response = controller.redirectToPaymentPage(token);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).contains(checkoutUrl);
        // URL encoding should be handled properly
        assertThat(location.toString()).contains("token=");
    }

    @Test
    void decodePaymentToken_WithComplexPaymentInfo_ReturnsAllFields() {
        // given
        String token = "complex-jwt-token";
        UUID orderId = UUID.randomUUID();
        PaymentTokenService.PaymentTokenInfo complexInfo =
                PaymentTokenService.PaymentTokenInfo.builder()
                        .orderId(orderId)
                        .amount(999999)
                        .customerKey("customer-with-long-key-123456")
                        .orderNo("복잡한주문명-001")
                        .successUrl("https://example.com/success?param=value")
                        .failUrl("https://example.com/fail?error=true")
                        .build();

        when(paymentTokenService.parsePaymentToken(token)).thenReturn(complexInfo);

        // when
        ResponseEntity<BaseResponse<PaymentTokenService.PaymentTokenInfo>> response =
                controller.decodePaymentToken(token);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        PaymentTokenService.PaymentTokenInfo data = response.getBody().getData();
        assertThat(data.getOrderId()).isEqualTo(orderId);
        assertThat(data.getAmount()).isEqualTo(999999);
        assertThat(data.getCustomerKey()).isEqualTo("customer-with-long-key-123456");
        assertThat(data.getOrderNo()).contains("복잡한주문명");
        assertThat(data.getSuccessUrl()).contains("success");
        assertThat(data.getFailUrl()).contains("fail");
    }
}