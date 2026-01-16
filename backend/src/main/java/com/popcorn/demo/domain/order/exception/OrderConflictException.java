package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.common.exception.BaseException;

public class OrderConflictException extends BaseException {

	private OrderConflictException(OrderResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static OrderConflictException duplicateIdempotencyKey() {
		return new OrderConflictException(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY);
	}

	public static OrderConflictException duplicateOrder() {
		return new OrderConflictException(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY); // 기존 코드와 호환성을 위해 같은 코드 사용
	}

	public static OrderConflictException alreadyCanceled() {
		return new OrderConflictException(OrderResponseCode.ALREADY_CANCELED);
	}
}
