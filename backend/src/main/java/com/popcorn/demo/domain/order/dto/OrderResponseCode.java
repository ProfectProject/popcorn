package com.popcorn.demo.domain.order.dto;

import lombok.Getter;

/**
 * 주문 도메인 전용 응답 코드
 *
 * 주문과 관련된 모든 비즈니스 로직의 응답 코드를 정의합니다.
 * 코드 범위: 1000 ~ 1999 (Order Domain)
 */
@Getter
public enum OrderResponseCode implements com.popcorn.demo.common.dto.ResponseCode {

    // 주문 생성 관련 오류 (1000~1099)
    EMPTY_ITEMS(1000, 400, "주문 항목이 비어있습니다."),

    INVALID_QTY(1001, 400, "수량은 1 이상이어야 합니다."),

    MIXED_ORDER_ITEMS_NOT_ALLOWED(1002, 400, "예약과 구매 항목을 함께 주문할 수 없습니다."),

    ORDER_TYPE_ITEM_MISMATCH(1003, 400, "주문 타입과 항목 타입이 일치하지 않습니다."),

    CHECKIN_REQUIRED(1004, 400, "체크인이 필요합니다."),

    SESSION_NOT_OPEN(1005, 400, "세션이 열려있지 않습니다."),

    OPTION_NOT_ALLOWED_FOR_SESSION(1006, 400, "세션에 허용되지 않은 옵션입니다."),

    // 리소스 없음 관련 오류 (1100~1199)
    ORDER_NOT_FOUND(1100, 404, "주문을 찾을 수 없습니다."),

    STORE_NOT_FOUND(1101, 404, "스토어를 찾을 수 없습니다."),

    PRODUCT_NOT_FOUND(1102, 404, "상품을 찾을 수 없습니다."),

    SESSION_NOT_FOUND(1103, 404, "세션을 찾을 수 없습니다."),

    OPTION_NOT_FOUND(1104, 404, "옵션을 찾을 수 없습니다."),

    MERCH_VARIANT_NOT_FOUND(1105, 404, "상품 변형을 찾을 수 없습니다."),

    // 상태/권한 관련 오류 (1200~1299)
    PRODUCT_HIDDEN(1200, 403, "숨김 처리된 상품입니다."),

    INVALID_STATUS_TRANSITION(1201, 400, "허용되지 않은 상태 변경입니다."),

    // 충돌/중복 관련 오류 (1300~1399)
    CAPACITY_EXCEEDED(1300, 409, "정원을 초과했습니다."),

    OUT_OF_STOCK(1301, 409, "재고가 부족합니다."),

    DUPLICATE_IDEMPOTENCY_KEY(1302, 409, "중복된 요청입니다."),

    ALREADY_CANCELED(1303, 409, "이미 취소된 주문입니다."),

    PAYMENT_ALREADY_EXISTS(1304, 409, "해당 주문에 결제 기록이 이미 존재합니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    OrderResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
