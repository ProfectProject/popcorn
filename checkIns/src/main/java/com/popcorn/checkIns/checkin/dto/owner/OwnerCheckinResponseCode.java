package com.popcorn.checkIns.checkin.dto.owner;

import com.popcorn.common.dto.ResponseCode;

import lombok.Getter;

@Getter
public enum OwnerCheckinResponseCode implements ResponseCode {

	UNAUTHENTICATED(2601, 401, "인증 정보가 없습니다."),
	USER_ID_REQUIRED(2602, 400, "사용자 ID가 필요합니다."),
	INVALID_PRINCIPAL(2603, 400, "인증 정보가 올바르지 않습니다."),
	INVALID_ROLE(2604, 400, "권한 정보가 올바르지 않습니다."),
	USER_NOT_OWNER(2605, 403, "오너 권한이 필요합니다."),
	POPUP_ID_REQUIRED(2606, 400, "팝업 ID는 필수입니다."),
	SCHEDULE_ID_REQUIRED(2607, 400, "스케줄 ID는 필수입니다."),
	SCHEDULE_NOT_FOUND(2608, 404, "스케줄을 찾을 수 없습니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	OwnerCheckinResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
