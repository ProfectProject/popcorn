package com.popcorn.store.domain.popup.exception.manager;

import com.popcorn.common.exception.BaseException;
import com.popcorn.store.domain.popup.dto.manager.ManagerPopupResponseCode;

public class ManagerPopupException extends BaseException {

    private ManagerPopupException(ManagerPopupResponseCode responseCode) {
        super(responseCode, "매니저 팝업 처리 중 오류가 발생했습니다.");
    }

    public static ManagerPopupException unauthenticated() {
        return new ManagerPopupException(ManagerPopupResponseCode.UNAUTHENTICATED);
    }

    public static ManagerPopupException userIdRequired() {
        return new ManagerPopupException(ManagerPopupResponseCode.USER_ID_REQUIRED);
    }

    public static ManagerPopupException invalidPrincipal() {
        return new ManagerPopupException(ManagerPopupResponseCode.INVALID_PRINCIPAL);
    }

    public static ManagerPopupException invalidRole() {
        return new ManagerPopupException(ManagerPopupResponseCode.INVALID_ROLE);
    }

    public static ManagerPopupException notManager() {
        return new ManagerPopupException(ManagerPopupResponseCode.USER_NOT_MANAGER);
    }

    public static ManagerPopupException managerIdRequired() {
        return new ManagerPopupException(ManagerPopupResponseCode.MANAGER_ID_REQUIRED);
    }

    public static ManagerPopupException popupAlreadyInactive() {
        return new ManagerPopupException(ManagerPopupResponseCode.POPUP_ALREADY_INACTIVE);
    }

    public static ManagerPopupException popupNotInRequest() {
        return new ManagerPopupException(ManagerPopupResponseCode.POPUP_NOT_IN_REQUEST);
    }

    public static ManagerPopupException popupNotApproved() {
        return new ManagerPopupException(ManagerPopupResponseCode.POPUP_NOT_APPROVED);
    }
}
