package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OrderNotFoundException extends BaseException {

	private OrderNotFoundException(OrderResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
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
