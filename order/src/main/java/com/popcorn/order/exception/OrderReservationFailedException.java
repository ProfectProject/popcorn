package com.popcorn.order.exception;

public class OrderReservationFailedException extends RuntimeException {
    public OrderReservationFailedException(String message) {
        super(message);
    }
}
