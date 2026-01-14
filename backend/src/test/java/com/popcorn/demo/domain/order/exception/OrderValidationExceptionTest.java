package com.popcorn.demo.domain.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;

class OrderValidationExceptionTest {

    @Test
    void invalidRequestCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.invalidRequest();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void emptyItemsCreatesExceptionWithEmptyItemsCode() {
        // When
        OrderValidationException exception = OrderValidationException.emptyItems();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.EMPTY_ITEMS);
    }

    @Test
    void invalidQtyCreatesExceptionWithInvalidQtyCode() {
        // When
        OrderValidationException exception = OrderValidationException.invalidQty();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.INVALID_QTY);
    }

    @Test
    void invalidStatusTransitionCreatesExceptionWithInvalidStatusTransitionCode() {
        // When
        OrderValidationException exception = OrderValidationException.invalidStatusTransition();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void tooManyItemsCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.tooManyItems();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void quantityLimitExceededCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.quantityLimitExceeded();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void belowMinimumOrderAmountCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.belowMinimumOrderAmount();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void aboveMaximumOrderAmountCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.aboveMaximumOrderAmount();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void invalidOrderItemCombinationCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.invalidOrderItemCombination();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void orderOutsideBusinessHoursCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.orderOutsideBusinessHours();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void orderCannotBeCancelledCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.orderCannotBeCancelled();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void cancellationTimeExpiredCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.cancellationTimeExpired();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }

    @Test
    void reasonRequiredForRejectionOrCancellationCreatesExceptionWithInvalidRequestCode() {
        // When
        OrderValidationException exception = OrderValidationException.reasonRequiredForRejectionOrCancellation();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
    }
}