package com.popcorn.demo.domain.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.dto.OrderResponseCode;

class OrderNotFoundExceptionTest {

    @Test
    void orderNotFoundCreatesExceptionWithOrderNotFoundCode() {
        // When
        OrderNotFoundException exception = OrderNotFoundException.orderNotFound();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.ORDER_NOT_FOUND);
    }

    @Test
    void storeNotFoundCreatesExceptionWithStoreNotFoundCode() {
        // When
        OrderNotFoundException exception = OrderNotFoundException.storeNotFound();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.STORE_NOT_FOUND);
    }

    @Test
    void productNotFoundCreatesExceptionWithProductNotFoundCode() {
        // When
        OrderNotFoundException exception = OrderNotFoundException.productNotFound();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void sessionNotFoundCreatesExceptionWithSessionNotFoundCode() {
        // When
        OrderNotFoundException exception = OrderNotFoundException.sessionNotFound();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.SESSION_NOT_FOUND);
    }

    @Test
    void optionNotFoundCreatesExceptionWithOptionNotFoundCode() {
        // When
        OrderNotFoundException exception = OrderNotFoundException.optionNotFound();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.OPTION_NOT_FOUND);
    }

    @Test
    void merchVariantNotFoundCreatesExceptionWithMerchVariantNotFoundCode() {
        // When
        OrderNotFoundException exception = OrderNotFoundException.merchVariantNotFound();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.MERCH_VARIANT_NOT_FOUND);
    }
}