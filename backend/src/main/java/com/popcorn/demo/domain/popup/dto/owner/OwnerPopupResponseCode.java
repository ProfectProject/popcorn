package com.popcorn.demo.domain.popup.dto.owner;

import com.popcorn.demo.common.dto.ResponseCode;
import lombok.Getter;

@Getter
public enum OwnerPopupResponseCode implements ResponseCode {

	UNAUTHENTICATED(2401, 401, "인증 정보가 없습니다."),
	USER_ID_REQUIRED(2402, 400, "사용자 ID가 필요합니다."),
	INVALID_PRINCIPAL(2403, 400, "인증 정보가 올바르지 않습니다."),
	INVALID_ROLE(2404, 400, "권한 정보가 올바르지 않습니다."),
	USER_NOT_OWNER(2405, 403, "오너 권한이 필요합니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	OwnerPopupResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
