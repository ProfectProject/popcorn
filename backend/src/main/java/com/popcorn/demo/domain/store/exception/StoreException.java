package com.popcorn.demo.domain.store.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.store.dto.StoreResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class StoreException extends BaseException {

    private StoreException(CommonResponseCode responseCode) {
        super(responseCode);
    }

    private StoreException(CommonResponseCode responseCode, Throwable cause) {
        super(responseCode, cause);
    }

    private StoreException(StoreResponseCode responseCode) {
        super(responseCode);
    }

    private StoreException(StoreResponseCode responseCode, Throwable cause) {
        super(responseCode, cause);
    }

    // ================ 검증 실패 예외들 (400 Bad Request) ================

    public static StoreException invalidRequest() {
        return new StoreException(CommonResponseCode.INVALID_REQUEST);
    }

    public static StoreException emptyName() {
        return new StoreException(StoreResponseCode.EMPTY_NAME);
    }

    public static StoreException invalidNameLength(int actualLength, int minLength, int maxLength) {
        return new StoreException(StoreResponseCode.INVALID_NAME_LENGTH);
    }

    public static StoreException invalidNameFormat(String storeName) {
        return new StoreException(StoreResponseCode.INVALID_NAME_FORMAT);
    }

    public static StoreException storeCreationLimitExceeded(Long ownerId, int maxStores) {
        return new StoreException(StoreResponseCode.STORE_CREATION_LIMIT_EXCEEDED);
    }
    
    public static StoreException storeIdRequired() {
        return new StoreException(StoreResponseCode.STORE_ID_REQUIRED);
    }
    
    public static StoreException userIdRequired() {
        return new StoreException(StoreResponseCode.USER_ID_REQUIRED);
    }

    // ================ 리소스 없음 예외들 (404 Not Found) ================

    public static StoreException ownerNotFound() {
        return new StoreException(StoreResponseCode.OWNER_NOT_FOUND);
    }

    public static StoreException storeNotFound(Object storeId) {
        return new StoreException(StoreResponseCode.STORE_NOT_FOUND);
    }

    // ================ 비즈니스 규칙 위반 예외들 (409 Conflict) ================

    public static StoreException duplicateStoreName(String storeName) {
        return new StoreException(StoreResponseCode.DUPLICATE_STORE_NAME);
    }

    public static StoreException ownerNotAuthorized(Long ownerId) {
        return new StoreException(StoreResponseCode.OWNER_NOT_AUTHORIZED);
    }

    public static StoreException accessDenied(Long userId, Object storeId) {
        return new StoreException(StoreResponseCode.ACCESS_DENIED);
    }

    public static StoreException storeAlreadyDeleted(Object storeId) {
        return new StoreException(StoreResponseCode.STORE_ALREADY_DELETED);
    }

    // ================ 시스템 오류 예외들 (500 Internal Server Error) ================

    public static StoreException validationTimeout() {
        return new StoreException(StoreResponseCode.VALIDATION_TIMEOUT);
    }

    public static StoreException validationInterrupted() {
        return new StoreException(StoreResponseCode.VALIDATION_INTERRUPTED);
    }

    public static StoreException validationFailed(String message, Throwable cause) {
        return new StoreException(StoreResponseCode.VALIDATION_FAILED, cause);
    }

}
