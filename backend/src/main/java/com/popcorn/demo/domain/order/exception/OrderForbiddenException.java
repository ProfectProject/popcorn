package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OrderForbiddenException extends BaseException {

	private OrderForbiddenException(CommonResponseCode responseCode) {
		super(responseCode);
	}

	public static OrderForbiddenException forbidden() {
		return new OrderForbiddenException(CommonResponseCode.FORBIDDEN);
	}

	public static OrderForbiddenException customerCannotCancelOrder() {
		return new OrderForbiddenException(CommonResponseCode.FORBIDDEN);
	}
}
