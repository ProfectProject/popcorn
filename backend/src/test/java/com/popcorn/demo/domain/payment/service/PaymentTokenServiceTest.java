package com.popcorn.demo.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentTokenServiceTest {

    @Test
    void createAndParseToken() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentTokenService.PaymentTokenInfo info = PaymentTokenService.PaymentTokenInfo.builder()
                .orderId(orderId)
                .orderNo("O-1001")
                .amount(12000)
                .customerKey("1")
                .paymentId(paymentId)
                .build();

        String token = service.createPaymentToken(info);
        PaymentTokenService.PaymentTokenInfo parsed = service.parsePaymentToken(token);

        assertThat(parsed.getOrderId()).isEqualTo(orderId);
        assertThat(parsed.getOrderNo()).isEqualTo("O-1001");
        assertThat(parsed.getAmount()).isEqualTo(12000);
        assertThat(parsed.getCustomerKey()).isEqualTo("1");
        assertThat(parsed.getPaymentId()).isEqualTo(paymentId);
    }

    @Test
    void parseInvalidTokenThrows() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        assertThatThrownBy(() -> service.parsePaymentToken("bad-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isValidTokenDetectsInvalid() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        String token = service.createPaymentToken(PaymentTokenService.PaymentTokenInfo.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-2002")
                .amount(3000)
                .customerKey("2")
                .paymentId(UUID.randomUUID())
                .build());

        assertThat(service.isValidToken(token)).isTrue();
        assertThat(service.isValidToken("bad-token")).isFalse();
    }

    @Test
    void createTokenWithNullOrderId() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        UUID paymentId = UUID.randomUUID();
        PaymentTokenService.PaymentTokenInfo info = PaymentTokenService.PaymentTokenInfo.builder()
                .orderId(null) // null orderId
                .orderNo("O-5005")
                .amount(8000)
                .customerKey("5")
                .paymentId(paymentId)
                .build();

        String token = service.createPaymentToken(info);
        PaymentTokenService.PaymentTokenInfo parsed = service.parsePaymentToken(token);

        assertThat(parsed.getOrderId()).isNull();
        assertThat(parsed.getOrderNo()).isEqualTo("O-5005");
        assertThat(parsed.getAmount()).isEqualTo(8000);
        assertThat(parsed.getPaymentId()).isEqualTo(paymentId);
    }

    @Test
    void parseTokenSetsSuccessAndFailUrls() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        String token = service.createPaymentToken(PaymentTokenService.PaymentTokenInfo.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-URL-TEST")
                .amount(1000)
                .customerKey("url-test")
                .paymentId(UUID.randomUUID())
                .build());

        PaymentTokenService.PaymentTokenInfo parsed = service.parsePaymentToken(token);

        assertThat(parsed.getSuccessUrl()).isEqualTo("http://localhost:3000/payments/success");
        assertThat(parsed.getFailUrl()).isEqualTo("http://localhost:3000/payments/fail");
    }

    @Test
    void parseTokenHandlesBlankOrderId() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        // Create token manually with blank orderId to test the blank check
        String tokenWithBlankOrderId = service.createPaymentToken(PaymentTokenService.PaymentTokenInfo.builder()
                .orderId(null) // This will result in null claim
                .orderNo("O-BLANK-TEST")
                .amount(500)
                .customerKey("blank-test")
                .paymentId(UUID.randomUUID())
                .build());

        PaymentTokenService.PaymentTokenInfo parsed = service.parsePaymentToken(tokenWithBlankOrderId);
        assertThat(parsed.getOrderId()).isNull();
    }

    @Test
    void isValidTokenHandlesNullAndEmptyTokens() {
        PaymentTokenService service = new PaymentTokenService();
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key-that-is-long-enough-for-jwt-security-requirements");

        assertThat(service.isValidToken(null)).isFalse();
        assertThat(service.isValidToken("")).isFalse();
        assertThat(service.isValidToken("   ")).isFalse();
    }
}
