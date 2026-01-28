package com.popcorn.order.exception;

public class OrderReservationTimeoutException extends RuntimeException {
    public OrderReservationTimeoutException(String message) {
        super(message);
    }
}
