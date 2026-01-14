package com.popcorn.demo.domain.checkin.exception;

import com.popcorn.demo.domain.checkin.dto.CheckinResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class CheckinException extends BaseException {

	private CheckinException(CheckinResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static CheckinException notFound() {
		return new CheckinException(CheckinResponseCode.CHECKIN_NOT_FOUND);
	}
}
