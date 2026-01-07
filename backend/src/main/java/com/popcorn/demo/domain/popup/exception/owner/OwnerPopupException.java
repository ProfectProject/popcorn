package com.popcorn.demo.domain.popup.exception.owner;

import com.popcorn.demo.domain.popup.dto.owner.OwnerPopupResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class OwnerPopupException extends BaseException {

	private OwnerPopupException(OwnerPopupResponseCode responseCode) {
		super(responseCode);
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
}
