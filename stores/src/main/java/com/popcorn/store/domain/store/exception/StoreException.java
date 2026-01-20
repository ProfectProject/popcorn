package com.popcorn.store.domain.store.exception;

import com.popcorn.common.dto.CommonResponseCode;
import com.popcorn.store.domain.store.dto.StoreResponseCode;
import com.popcorn.common.exception.BaseException;

public class StoreException extends BaseException {

    private StoreException(CommonResponseCode responseCode) {
        super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
    }

    private StoreException(CommonResponseCode responseCode, Throwable cause) {
        super(responseCode, cause);
    }

    private StoreException(StoreResponseCode responseCode) {
        super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
    }

    private StoreException(StoreResponseCode responseCode, Throwable cause) {
        super(responseCode, cause);
    }

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

    public static StoreException notOwner() {
        return new StoreException(StoreResponseCode.USER_NOT_OWNER);
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

    public static StoreException ownerNotFound() {
        return new StoreException(StoreResponseCode.OWNER_NOT_FOUND);
    }

    public static StoreException storeNotFound(Object storeId) {
        return new StoreException(StoreResponseCode.STORE_NOT_FOUND);
    }

    public static StoreException unauthenticated() {
        return new StoreException(StoreResponseCode.UNAUTHENTICATED);
    }

    public static StoreException invalidPrincipal() {
        return new StoreException(StoreResponseCode.INVALID_PRINCIPAL);
    }

    public static StoreException invalidRole() {
        return new StoreException(StoreResponseCode.INVALID_ROLE);
    }


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
