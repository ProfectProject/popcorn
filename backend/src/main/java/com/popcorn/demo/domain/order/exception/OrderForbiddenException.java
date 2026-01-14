package com.popcorn.demo.domain.order.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OrderForbiddenException extends BaseException {

	private OrderForbiddenException(CommonResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static OrderForbiddenException forbidden() {
		return new OrderForbiddenException(CommonResponseCode.FORBIDDEN);
	}

	public static OrderForbiddenException customerCannotCancelOrder() {
		return new OrderForbiddenException(CommonResponseCode.FORBIDDEN);
	}
}
