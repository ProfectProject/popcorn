package com.popcorn.common.dto;

import lombok.Getter;

/**
 * 공통 응답 코드
 *
 * 모든 도메인에서 공통적으로 사용하는 기본적인 응답 코드들을 정의합니다.
 */
@Getter
public enum CommonResponseCode implements ResponseCode {

    SUCCESS(200, 200, "요청이 성공했습니다."),

    INVALID_REQUEST(400, 400, "잘못된 요청입니다."),

    NOT_FOUND(404, 404, "리소스를 찾을 수 없습니다."),

    FORBIDDEN(403, 403, "권한이 없습니다."),

    INTERNAL_ERROR(500, 500, "서버 오류가 발생했습니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    CommonResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}