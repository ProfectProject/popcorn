package com.popcorn.store.domain.popup.dto;

import com.popcorn.demo.common.dto.ResponseCode;

import lombok.Getter;

@Getter
public enum PopupResponseCode implements ResponseCode {

	INVALID_REQUEST(2100, 400, "잘못된 요청입니다."),
	POPUP_NOT_FOUND(2101, 404, "팝업 정보를 찾을 수 없습니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	PopupResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
