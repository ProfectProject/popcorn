package com.example.orderquery.domain.itemView.exception;

import com.popcorn.common.dto.ResponseCode;
import lombok.Getter;

@Getter
public enum ItemViewResponseCode implements ResponseCode {

    ITEMS_NOT_FOUND(1404, 404, "주문 항목을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(1400, 400, "orderStatus 값이 유효하지 않습니다."),
    INVALID_PAYMENT_STATUS(1401, 400, "paymentStatus 값이 유효하지 않습니다."),
    INVALID_DATE_RANGE(1402, 400, "from/to 범위가 유효하지 않습니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    ItemViewResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
