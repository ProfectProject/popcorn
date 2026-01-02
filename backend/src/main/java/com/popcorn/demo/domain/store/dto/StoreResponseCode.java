package com.popcorn.demo.domain.store.dto;

import lombok.Getter;

@Getter
public enum StoreResponseCode {

    // Store creation specific error codes
    EMPTY_NAME(400, 400, "이름이 비어있습니다."),
    EMPTY_OWNER_ID(400, 400, "오너 ID가 비어있습니다."),
    OWNER_NOT_FOUND(404, 404, "오너를 찾을 수 없습니다."),
    DUPLICATE_STORE_NAME(409, 409, "중복된 스토어 이름입니다."),
    OWNER_NOT_AUTHORIZED(403, 403, "스토어 생성 권한이 없습니다."),
    INVALID_NAME_FORMAT(400, 400, "스토어 이름 형식이 유효하지 않습니다."),
    VALIDATION_TIMEOUT(408, 408, "검증 시간이 초과되었습니다."),
    VALIDATION_INTERRUPTED(500, 500, "검증이 중단되었습니다."),
    VALIDATION_FAILED(500, 500, "검증에 실패했습니다."),
    STORE_NOT_FOUND(404, 404, "스토어를 찾을 수 없습니다."),
    STORE_ALREADY_DELETED(410, 410, "이미 삭제된 스토어입니다."),
    ACCESS_DENIED(403, 403, "접근 권한이 없습니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    StoreResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

}
