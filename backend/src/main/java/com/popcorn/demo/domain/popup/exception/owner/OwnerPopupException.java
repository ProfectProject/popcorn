package com.popcorn.demo.domain.popup.exception.owner;

import com.popcorn.demo.domain.popup.dto.owner.OwnerPopupResponseCode;
import com.popcorn.common.exception.BaseException;

public class OwnerPopupException extends BaseException {

	private OwnerPopupException(OwnerPopupResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static OwnerPopupException unauthenticated() {
		return new OwnerPopupException(OwnerPopupResponseCode.UNAUTHENTICATED);
	}

	public static OwnerPopupException userIdRequired() {
		return new OwnerPopupException(OwnerPopupResponseCode.USER_ID_REQUIRED);
	}

	public static OwnerPopupException invalidPrincipal() {
		return new OwnerPopupException(OwnerPopupResponseCode.INVALID_PRINCIPAL);
	}

	public static OwnerPopupException invalidRole() {
		return new OwnerPopupException(OwnerPopupResponseCode.INVALID_ROLE);
	}

	public static OwnerPopupException notOwner() {
		return new OwnerPopupException(OwnerPopupResponseCode.USER_NOT_OWNER);
	}

	public static OwnerPopupException of(OwnerPopupResponseCode responseCode) {
		return new OwnerPopupException(responseCode);
	}
}
