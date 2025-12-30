package com.popcorn.demo.common.dto;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("SUCCESS", "요청이 성공했습니다."),
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND("NOT_FOUND", "리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다."),

    // Order creation specific error codes
    EMPTY_ITEMS("EMPTY_ITEMS", "주문 항목이 비어있습니다."),
    INVALID_QTY("INVALID_QTY", "수량은 1 이상이어야 합니다."),
    MIXED_ORDER_ITEMS_NOT_ALLOWED("MIXED_ORDER_ITEMS_NOT_ALLOWED", "예약과 구매 항목을 함께 주문할 수 없습니다."),
    ORDER_TYPE_ITEM_MISMATCH("ORDER_TYPE_ITEM_MISMATCH", "주문 타입과 항목 타입이 일치하지 않습니다."),
    STORE_NOT_FOUND("STORE_NOT_FOUND", "스토어를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."),
    OPTION_NOT_FOUND("OPTION_NOT_FOUND", "옵션을 찾을 수 없습니다."),
    MERCH_VARIANT_NOT_FOUND("MERCH_VARIANT_NOT_FOUND", "상품 변형을 찾을 수 없습니다."),
    PRODUCT_HIDDEN("PRODUCT_HIDDEN", "숨김 처리된 상품입니다."),
    SESSION_NOT_OPEN("SESSION_NOT_OPEN", "세션이 열려있지 않습니다."),
    OPTION_NOT_ALLOWED_FOR_SESSION("OPTION_NOT_ALLOWED_FOR_SESSION", "세션에 허용되지 않은 옵션입니다."),
    CAPACITY_EXCEEDED("CAPACITY_EXCEEDED", "정원을 초과했습니다."),
    OUT_OF_STOCK("OUT_OF_STOCK", "재고가 부족합니다."),
    CHECKIN_REQUIRED("CHECKIN_REQUIRED", "체크인이 필요합니다."),
    DUPLICATE_IDEMPOTENCY_KEY("DUPLICATE_IDEMPOTENCY_KEY", "중복된 요청입니다."),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.");

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
