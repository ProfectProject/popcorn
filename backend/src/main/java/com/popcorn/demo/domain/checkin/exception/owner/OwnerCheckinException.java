package com.popcorn.demo.domain.checkin.exception.owner;

import com.popcorn.demo.domain.checkin.dto.owner.OwnerCheckinResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OwnerCheckinException extends BaseException {

	private OwnerCheckinException(OwnerCheckinResponseCode responseCode) {
		super(responseCode);
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
}
