package com.popcorn.store.domain.popup.dto.manager;

import com.popcorn.common.dto.ResponseCode;

import lombok.Getter;

@Getter
public enum ManagerPopupResponseCode implements ResponseCode {

    UNAUTHENTICATED(2501, 401, "인증 정보가 없습니다."),
    USER_ID_REQUIRED(2502, 400, "사용자 ID가 필요합니다."),
    INVALID_PRINCIPAL(2503, 400, "인증 정보가 올바르지 않습니다."),
    INVALID_ROLE(2504, 400, "권한 정보가 올바르지 않습니다."),
    USER_NOT_MANAGER(2505, 403, "매니저 권한이 필요합니다."),
    MANAGER_ID_REQUIRED(2506, 400, "매니저 ID는 필수입니다."),
    POPUP_ALREADY_INACTIVE(2507, 400, "이미 중단된 팝업입니다."),
    POPUP_NOT_IN_REQUEST(2508, 400, "승인 대기 상태가 아닙니다."),
    POPUP_NOT_APPROVED(2509, 400, "승인된 팝업이 아닙니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    ManagerPopupResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
