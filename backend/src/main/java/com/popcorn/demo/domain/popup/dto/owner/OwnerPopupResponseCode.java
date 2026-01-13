package com.popcorn.demo.domain.popup.dto.owner;

import com.popcorn.demo.common.dto.ResponseCode;
import lombok.Getter;

@Getter
public enum OwnerPopupResponseCode implements ResponseCode {

	UNAUTHENTICATED(2401, 401, "인증 정보가 없습니다."),
	USER_ID_REQUIRED(2402, 400, "사용자 ID가 필요합니다."),
	INVALID_PRINCIPAL(2403, 400, "인증 정보가 올바르지 않습니다."),
	INVALID_ROLE(2404, 400, "권한 정보가 올바르지 않습니다."),
	USER_NOT_OWNER(2405, 403, "오너 권한이 필요합니다."),

	STORE_ID_REQUIRED(2410, 400, "스토어 ID는 필수입니다."),
	CATEGORY_REQUIRED(2411, 400, "팝업 카테고리는 필수입니다."),
	TITLE_REQUIRED(2412, 400, "팝업 제목은 필수입니다."),
	TITLE_LENGTH_INVALID(2413, 400, "팝업 제목은 1-200자 사이여야 합니다."),
	TITLE_INVALID_CHARS(2414, 400, "팝업 제목에 허용되지 않는 문자가 포함되어 있습니다."),
	SCHEDULES_REQUIRED(2415, 400, "팝업 스케줄은 최소 1개 이상 필요합니다."),
	SCHEDULE_REQUIRED(2416, 400, "스케줄 정보가 비어 있습니다."),
	SCHEDULE_ID_REQUIRED(2417, 400, "스케줄 ID는 필수입니다."),
	SCHEDULE_DELETE_ID_REQUIRED(2418, 400, "삭제할 스케줄 ID는 필수입니다."),
	START_AT_REQUIRED(2419, 400, "시작 시간은 필수입니다."),
	END_AT_REQUIRED(2420, 400, "종료 시간은 필수입니다."),
	END_BEFORE_START(2421, 400, "종료 시간은 시작 시간 이후여야 합니다."),
	SCHEDULE_TIME_PAIR_REQUIRED(2422, 400, "시작/종료 시간은 함께 입력해야 합니다."),
	PRICE_REQUIRED(2423, 400, "가격은 필수입니다."),
	PRICE_MIN_INVALID(2424, 400, "가격은 0 이상이어야 합니다."),
	CAPACITY_REQUIRED(2425, 400, "수용 인원은 필수입니다."),
	CAPACITY_MIN_INVALID(2426, 400, "수용 인원은 1 이상이어야 합니다."),
	UPDATE_NO_CHANGES(2427, 400, "수정할 내용이 없습니다."),
	REQUEST_BODY_REQUIRED(2428, 400, "요청 본문이 비어 있습니다."),
	SCHEDULE_UPDATE_NOT_FOUND(2429, 400, "수정할 스케줄을 찾을 수 없습니다."),
	SCHEDULE_DELETE_NOT_FOUND(2430, 400, "삭제할 스케줄을 찾을 수 없습니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	OwnerPopupResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
