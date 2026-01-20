package com.popcorn.demo.domain.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.popcorn.common.dto.CommonResponseCode;

class OrderForbiddenExceptionTest {

    @Test
    void forbiddenCreatesExceptionWithForbiddenCode() {
        // When
        OrderForbiddenException exception = OrderForbiddenException.forbidden();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.FORBIDDEN);
    }

    @Test
    void customerCannotCancelOrderCreatesExceptionWithForbiddenCode() {
        // When
        OrderForbiddenException exception = OrderForbiddenException.customerCannotCancelOrder();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.FORBIDDEN);
    }
}