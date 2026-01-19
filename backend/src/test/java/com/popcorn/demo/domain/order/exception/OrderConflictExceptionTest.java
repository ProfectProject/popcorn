package com.popcorn.demo.domain.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.dto.OrderResponseCode;

class OrderConflictExceptionTest {

    @Test
    void duplicateIdempotencyKeyCreatesExceptionWithDuplicateIdempotencyKeyCode() {
        // When
        OrderConflictException exception = OrderConflictException.duplicateIdempotencyKey();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY);
    }

    @Test
    void duplicateOrderCreatesExceptionWithDuplicateIdempotencyKeyCode() {
        // When
        OrderConflictException exception = OrderConflictException.duplicateOrder();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY);
    }

    @Test
    void alreadyCanceledCreatesExceptionWithAlreadyCanceledCode() {
        // When
        OrderConflictException exception = OrderConflictException.alreadyCanceled();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.ALREADY_CANCELED);
    }
}