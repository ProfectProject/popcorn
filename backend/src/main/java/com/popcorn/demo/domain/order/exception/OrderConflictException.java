package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OrderConflictException extends BaseException {

	private OrderConflictException(OrderResponseCode responseCode) {
		super(responseCode);
	}

	public static OrderConflictException duplicateIdempotencyKey() {
		return new OrderConflictException(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY);
	}

	public static OrderConflictException alreadyCanceled() {
		return new OrderConflictException(OrderResponseCode.ALREADY_CANCELED);
	}
}
