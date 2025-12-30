package com.popcorn.demo.common.dto;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("SUCCESS", "요청이 성공했습니다."),
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND("NOT_FOUND", "리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
