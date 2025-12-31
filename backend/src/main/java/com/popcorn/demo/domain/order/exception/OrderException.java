package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;

/**

	* 주문 도메인 예외 클래스

	* Common 패키지의 BaseException을 상속받아 표준화된 예외 처리를 제공합니다.

	*

	* 제공하는 정적 팩토리 메서드들:

	* - 검증 실패 예외들 (400 Bad Request)

	* - 리소스 없음 예외들 (404 Not Found)

	* - 비즈니스 규칙 위반 예외들 (409 Conflict)

	*/

public class OrderException extends BaseException {



	private OrderException(ResponseCode responseCode) {

		super(responseCode);

	}



	private OrderException(ResponseCode responseCode, Throwable cause) {

		super(responseCode, cause);

	}



	// ================ 검증 실패 예외들 (400 Bad Request) ================



	public static OrderException invalidRequest() {

		return new OrderException(ResponseCode.INVALID_REQUEST);

	}



	public static OrderException emptyItems() {

		return new OrderException(ResponseCode.EMPTY_ITEMS);

	}



	public static OrderException invalidQty() {

		return new OrderException(ResponseCode.INVALID_QTY);

	}



	public static OrderException mixedOrderItemsNotAllowed() {

		return new OrderException(ResponseCode.MIXED_ORDER_ITEMS_NOT_ALLOWED);

	}



	public static OrderException orderTypeItemMismatch() {

		return new OrderException(ResponseCode.ORDER_TYPE_ITEM_MISMATCH);

	}



	// ================ 리소스 없음 예외들 (404 Not Found) ================



	public static OrderException storeNotFound() {

		return new OrderException(ResponseCode.STORE_NOT_FOUND);

	}



	public static OrderException productNotFound() {

		return new OrderException(ResponseCode.PRODUCT_NOT_FOUND);

	}



	public static OrderException sessionNotFound() {

		return new OrderException(ResponseCode.SESSION_NOT_FOUND);

	}



	public static OrderException optionNotFound() {

		return new OrderException(ResponseCode.OPTION_NOT_FOUND);

	}



	public static OrderException merchVariantNotFound() {

		return new OrderException(ResponseCode.MERCH_VARIANT_NOT_FOUND);

	}



	// ================ 비즈니스 규칙 위반 예외들 (409 Conflict) ================



	public static OrderException productHidden() {

		return new OrderException(ResponseCode.PRODUCT_HIDDEN);

	}



	public static OrderException sessionNotOpen() {

		return new OrderException(ResponseCode.SESSION_NOT_OPEN);

	}



	public static OrderException optionNotAllowedForSession() {

		return new OrderException(ResponseCode.OPTION_NOT_ALLOWED_FOR_SESSION);

	}



	public static OrderException capacityExceeded() {

		return new OrderException(ResponseCode.CAPACITY_EXCEEDED);

	}



	public static OrderException outOfStock() {

		return new OrderException(ResponseCode.OUT_OF_STOCK);

	}



	public static OrderException checkinRequired() {

		return new OrderException(ResponseCode.CHECKIN_REQUIRED);

	}



	public static OrderException duplicateIdempotencyKey() {

		return new OrderException(ResponseCode.DUPLICATE_IDEMPOTENCY_KEY);

	}



	public static OrderException forbidden() {

		return new OrderException(ResponseCode.FORBIDDEN);

	}

}

