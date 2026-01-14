package com.popcorn.demo.domain.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TossPaymentsPropertiesTest {

    @Test
    void propertiesCanBeSetAndRetrieved() {
        // Given
        TossPaymentsProperties properties = new TossPaymentsProperties();

        // When
        properties.setBaseUrl("https://api.tosspayments.com");
        properties.setSecretKey("test-secret-key");
        properties.setCheckoutUrl("https://checkout.tosspayments.com");
        properties.setSuccessUrl("https://example.com/success");
        properties.setFailUrl("https://example.com/fail");

        // Then
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.tosspayments.com");
        assertThat(properties.getSecretKey()).isEqualTo("test-secret-key");
        assertThat(properties.getCheckoutUrl()).isEqualTo("https://checkout.tosspayments.com");
        assertThat(properties.getSuccessUrl()).isEqualTo("https://example.com/success");
        assertThat(properties.getFailUrl()).isEqualTo("https://example.com/fail");
    }

    @Test
    void propertiesInitializeWithNullValues() {
        // Given & When
        TossPaymentsProperties properties = new TossPaymentsProperties();

        // Then
        assertThat(properties.getBaseUrl()).isNull();
        assertThat(properties.getSecretKey()).isNull();
        assertThat(properties.getCheckoutUrl()).isNull();
        assertThat(properties.getSuccessUrl()).isNull();
        assertThat(properties.getFailUrl()).isNull();
    }

    @Test
    void propertiesCanHandleEmptyStrings() {
        // Given
        TossPaymentsProperties properties = new TossPaymentsProperties();

        // When
        properties.setBaseUrl("");
        properties.setSecretKey("");
        properties.setCheckoutUrl("");
        properties.setSuccessUrl("");
        properties.setFailUrl("");

        // Then
        assertThat(properties.getBaseUrl()).isEqualTo("");
        assertThat(properties.getSecretKey()).isEqualTo("");
        assertThat(properties.getCheckoutUrl()).isEqualTo("");
        assertThat(properties.getSuccessUrl()).isEqualTo("");
        assertThat(properties.getFailUrl()).isEqualTo("");
    }
}