package com.popcorn.demo.domain.qr.dto;

import lombok.Getter;

import com.popcorn.demo.common.dto.ResponseCode;

/**
 * QR 도메인 전용 응답 코드
 *
 * 코드 범위: 2000 ~ 2099 (QR Domain)
 */
@Getter
public enum QrResponseCode implements ResponseCode {

	QR_NOT_FOUND(2000, 404, "QR 코드를 찾을 수 없습니다."),

	QR_EXPIRED(2001, 410, "QR 코드가 만료되었습니다."),

	ORDER_NOT_FOUND(2002, 404, "주문을 찾을 수 없습니다."),

	ORDER_NOT_RESERVED(2003, 409, "예약 상태의 주문만 QR 발급이 가능합니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	QrResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
