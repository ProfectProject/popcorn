package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;

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



	// 공통 응답 코드 생성자
	private OrderException(CommonResponseCode responseCode) {
		super(responseCode);
	}

	private OrderException(CommonResponseCode responseCode, Throwable cause) {
		super(responseCode, cause);
	}

	// Order 도메인 응답 코드 생성자
	private OrderException(OrderResponseCode responseCode) {
		super(responseCode);
	}

	private OrderException(OrderResponseCode responseCode, Throwable cause) {
		super(responseCode, cause);
	}



	// ================ 검증 실패 예외들 (400 Bad Request) ================



	public static OrderException invalidRequest() {
		return new OrderException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderException emptyItems() {
		return new OrderException(OrderResponseCode.EMPTY_ITEMS);
	}

	public static OrderException invalidQty() {
		return new OrderException(OrderResponseCode.INVALID_QTY);
	}

	public static OrderException mixedOrderItemsNotAllowed() {
		return new OrderException(OrderResponseCode.MIXED_ORDER_ITEMS_NOT_ALLOWED);
	}

	public static OrderException orderTypeItemMismatch() {
		return new OrderException(OrderResponseCode.ORDER_TYPE_ITEM_MISMATCH);
	}



	// ================ 리소스 없음 예외들 (404 Not Found) ================



	public static OrderException storeNotFound() {
		return new OrderException(OrderResponseCode.STORE_NOT_FOUND);
	}

	public static OrderException productNotFound() {
		return new OrderException(OrderResponseCode.PRODUCT_NOT_FOUND);
	}

	public static OrderException sessionNotFound() {
		return new OrderException(OrderResponseCode.SESSION_NOT_FOUND);
	}

	public static OrderException optionNotFound() {
		return new OrderException(OrderResponseCode.OPTION_NOT_FOUND);
	}

	public static OrderException merchVariantNotFound() {
		return new OrderException(OrderResponseCode.MERCH_VARIANT_NOT_FOUND);
	}



	// ================ 비즈니스 규칙 위반 예외들 (409 Conflict) ================



	public static OrderException productHidden() {
		return new OrderException(OrderResponseCode.PRODUCT_HIDDEN);
	}

	public static OrderException sessionNotOpen() {
		return new OrderException(OrderResponseCode.SESSION_NOT_OPEN);
	}

	public static OrderException optionNotAllowedForSession() {
		return new OrderException(OrderResponseCode.OPTION_NOT_ALLOWED_FOR_SESSION);
	}

	public static OrderException capacityExceeded() {
		return new OrderException(OrderResponseCode.CAPACITY_EXCEEDED);
	}

	public static OrderException outOfStock() {
		return new OrderException(OrderResponseCode.OUT_OF_STOCK);
	}

	public static OrderException checkinRequired() {
		return new OrderException(OrderResponseCode.CHECKIN_REQUIRED);
	}

	public static OrderException duplicateIdempotencyKey() {
		return new OrderException(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY);
	}

	public static OrderException forbidden() {
		return new OrderException(CommonResponseCode.FORBIDDEN);
	}

	public static OrderException orderNotFound() {
		return new OrderException(OrderResponseCode.ORDER_NOT_FOUND);
	}

	public static OrderException invalidStatusTransition() {
		return new OrderException(OrderResponseCode.INVALID_STATUS_TRANSITION);
	}

	public static OrderException alreadyCanceled() {
		return new OrderException(OrderResponseCode.ALREADY_CANCELED);
	}

}
