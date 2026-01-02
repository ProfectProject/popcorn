package com.popcorn.demo.domain.store.exception;

import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;

/**
 * 스토어 도메인 예외 클래스
 * - ResponseCode 기반의 예외 처리
 * - 정적 팩토리 메서드로 예외 생성
 */
public class StoreException extends BaseException {

    private StoreException(ResponseCode responseCode) {
        super(responseCode);
    }

    private StoreException(ResponseCode responseCode, Throwable cause) {
        super(responseCode, cause);
    }

    // ========================= 기본 예외 =========================

    public static StoreException invalidRequest() {
        return new StoreException(ResponseCode.INVALID_REQUEST);
    }

    public static StoreException emptyName() {
        return new StoreException(ResponseCode.EMPTY_NAME);
    }

    public static StoreException ownerNotFound() {
        return new StoreException(ResponseCode.OWNER_NOT_FOUND);
    }

    // ========================= 스토어 생성 관련 예외 =========================

    public static StoreException duplicateStoreName(String storeName) {
        return new StoreException(ResponseCode.DUPLICATE_STORE_NAME);
    }

    public static StoreException ownerNotAuthorized(Long ownerId) {
        return new StoreException(ResponseCode.OWNER_NOT_AUTHORIZED);
    }

    public static StoreException invalidNameFormat(String storeName) {
        return new StoreException(ResponseCode.INVALID_NAME_FORMAT);
    }

    // ========================= 비동기 처리 관련 예외 =========================

    public static StoreException validationTimeout() {
        return new StoreException(ResponseCode.VALIDATION_TIMEOUT);
    }

    public static StoreException validationInterrupted() {
        return new StoreException(ResponseCode.VALIDATION_INTERRUPTED);
    }

    public static StoreException validationFailed(String message, Throwable cause) {
        return new StoreException(ResponseCode.VALIDATION_FAILED, cause);
    }

    // ========================= 스토어 조회/수정/삭제 관련 예외 =========================

    public static StoreException storeNotFound(Object storeId) {
        return new StoreException(ResponseCode.STORE_NOT_FOUND);
    }

    public static StoreException storeAlreadyDeleted(Object storeId) {
        return new StoreException(ResponseCode.STORE_ALREADY_DELETED);
    }

    public static StoreException accessDenied(Long userId, Object storeId) {
        return new StoreException(ResponseCode.ACCESS_DENIED);
    }

}
