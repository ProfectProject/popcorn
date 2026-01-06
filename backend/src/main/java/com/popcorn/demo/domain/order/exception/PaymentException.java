package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class PaymentException extends BaseException {

	private PaymentException(CommonResponseCode responseCode) {
		super(responseCode);
	}

	private PaymentException(OrderResponseCode responseCode) {
		super(responseCode);
	}

	public static PaymentException invalidRequest() {
		return new PaymentException(CommonResponseCode.INVALID_REQUEST);
	}

	public static PaymentException paymentAlreadyExists() {
		return new PaymentException(OrderResponseCode.PAYMENT_ALREADY_EXISTS);
	}
}
