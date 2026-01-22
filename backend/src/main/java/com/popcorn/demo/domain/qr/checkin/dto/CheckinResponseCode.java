package com.popcorn.demo.domain.qr.checkin.dto;

import com.popcorn.demo.common.dto.ResponseCode;

import lombok.Getter;

/**
 * 체크인 도메인 전용 응답 코드
 *
 * 코드 범위: 2100 ~ 2199 (Checkin Domain)
 */
@Getter
public enum CheckinResponseCode implements ResponseCode {

	CHECKIN_NOT_FOUND(2100, 404, "체크인을 찾을 수 없습니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	CheckinResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
