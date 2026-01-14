package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OrderValidationException extends BaseException {

	private OrderValidationException(CommonResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	private OrderValidationException(OrderResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static OrderValidationException invalidRequest() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException emptyItems() {
		return new OrderValidationException(OrderResponseCode.EMPTY_ITEMS);
	}

	public static OrderValidationException invalidQty() {
		return new OrderValidationException(OrderResponseCode.INVALID_QTY);
	}

	public static OrderValidationException invalidStatusTransition() {
		return new OrderValidationException(OrderResponseCode.INVALID_STATUS_TRANSITION);
	}

	public static OrderValidationException tooManyItems() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException quantityLimitExceeded() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException belowMinimumOrderAmount() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException aboveMaximumOrderAmount() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException invalidOrderItemCombination() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException orderOutsideBusinessHours() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException orderCannotBeCancelled() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException cancellationTimeExpired() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}

	public static OrderValidationException reasonRequiredForRejectionOrCancellation() {
		return new OrderValidationException(CommonResponseCode.INVALID_REQUEST);
	}
}
