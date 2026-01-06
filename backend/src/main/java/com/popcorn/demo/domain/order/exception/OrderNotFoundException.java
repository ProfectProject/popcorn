package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OrderNotFoundException extends BaseException {

	private OrderNotFoundException(OrderResponseCode responseCode) {
		super(responseCode);
	}

	public static OrderNotFoundException orderNotFound() {
		return new OrderNotFoundException(OrderResponseCode.ORDER_NOT_FOUND);
	}

	public static OrderNotFoundException storeNotFound() {
		return new OrderNotFoundException(OrderResponseCode.STORE_NOT_FOUND);
	}

	public static OrderNotFoundException productNotFound() {
		return new OrderNotFoundException(OrderResponseCode.PRODUCT_NOT_FOUND);
	}

	public static OrderNotFoundException sessionNotFound() {
		return new OrderNotFoundException(OrderResponseCode.SESSION_NOT_FOUND);
	}

	public static OrderNotFoundException optionNotFound() {
		return new OrderNotFoundException(OrderResponseCode.OPTION_NOT_FOUND);
	}

	public static OrderNotFoundException merchVariantNotFound() {
		return new OrderNotFoundException(OrderResponseCode.MERCH_VARIANT_NOT_FOUND);
	}
}
