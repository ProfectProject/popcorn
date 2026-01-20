package com.popcorn.checkIns.checkin.exception.owner;

import com.popcorn.checkIns.checkin.dto.owner.OwnerCheckinResponseCode;
import com.popcorn.demo.common.exception.BaseException;

public class OwnerCheckinException extends BaseException {

	private OwnerCheckinException(OwnerCheckinResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static OwnerCheckinException unauthenticated() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.UNAUTHENTICATED);
	}

	public static OwnerCheckinException userIdRequired() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.USER_ID_REQUIRED);
	}

	public static OwnerCheckinException invalidPrincipal() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.INVALID_PRINCIPAL);
	}

	public static OwnerCheckinException invalidRole() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.INVALID_ROLE);
	}

	public static OwnerCheckinException notOwner() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.USER_NOT_OWNER);
	}

	public static OwnerCheckinException popupIdRequired() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.POPUP_ID_REQUIRED);
	}

	public static OwnerCheckinException scheduleIdRequired() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.SCHEDULE_ID_REQUIRED);
	}

	public static OwnerCheckinException scheduleNotFound() {
		return new OwnerCheckinException(OwnerCheckinResponseCode.SCHEDULE_NOT_FOUND);
	}
}
