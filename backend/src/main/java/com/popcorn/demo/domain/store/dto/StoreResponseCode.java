package com.popcorn.demo.domain.store.dto;

import lombok.Getter;

// 스토어 도메인 전용 응답 코드.
@Getter
public enum StoreResponseCode implements com.popcorn.common.dto.ResponseCode {

    EMPTY_NAME(2000, 400, "이름이 비어있습니다."),
    
    INVALID_NAME_LENGTH(2001, 400, "스토어 이름 길이가 유효하지 않습니다."),
    
    INVALID_NAME_FORMAT(2002, 400, "스토어 이름 형식이 유효하지 않습니다."),
    
    STORE_CREATION_LIMIT_EXCEEDED(2003, 400, "스토어 생성 제한을 초과했습니다."),
    
    STORE_ID_REQUIRED(2004, 400, "스토어 ID는 필수입니다."),
    
    USER_ID_REQUIRED(2005, 400, "사용자 ID는 필수입니다."),

    OWNER_NOT_FOUND(2100, 404, "오너를 찾을 수 없습니다."),
    
    STORE_NOT_FOUND(2101, 404, "스토어를 찾을 수 없습니다."),

    OWNER_NOT_AUTHORIZED(2200, 403, "스토어 생성 권한이 없습니다."),
    
    ACCESS_DENIED(2201, 403, "접근 권한이 없습니다."),

    USER_NOT_OWNER(2202, 403, "사용자의 역할이 오너가 아닙니다."),

    UNAUTHENTICATED(2203, 401, "인증되지 않은 사용자입니다."),

    INVALID_PRINCIPAL(2204, 401, "잘못된 인증 정보입니다."),

    INVALID_ROLE(2205,403, "유효하지 않은 사용자 권한입니다."),

    DUPLICATE_STORE_NAME(2300, 409, "중복된 스토어 이름입니다."),
    
    STORE_ALREADY_DELETED(2301, 410, "이미 삭제된 스토어입니다."),

    VALIDATION_TIMEOUT(2500, 408, "검증 시간이 초과되었습니다."),
    
    VALIDATION_INTERRUPTED(2501, 500, "검증이 중단되었습니다."),
    
    VALIDATION_FAILED(2502, 500, "검증에 실패했습니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    StoreResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
