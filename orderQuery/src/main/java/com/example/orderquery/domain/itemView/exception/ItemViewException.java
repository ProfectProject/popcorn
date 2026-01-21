package com.example.orderquery.domain.itemView.exception;

import java.util.UUID;

import com.popcorn.common.exception.BaseException;

public class ItemViewException extends BaseException {

    private final String detail;

    private ItemViewException(ItemViewResponseCode responseCode, String detail) {
        super(responseCode, detail);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    public static ItemViewException itemsNotFound(UUID storeId, UUID popupId) {
        return new ItemViewException(ItemViewResponseCode.ITEMS_NOT_FOUND,
                "storeId=" + storeId + ", popupId=" + popupId);
    }

    public static ItemViewException invalidOrderStatus(String value) {
        return new ItemViewException(ItemViewResponseCode.INVALID_ORDER_STATUS,
                "orderStatus=" + value);
    }

    public static ItemViewException invalidPaymentStatus(String value) {
        return new ItemViewException(ItemViewResponseCode.INVALID_PAYMENT_STATUS,
                "paymentStatus=" + value);
    }

    public static ItemViewException invalidDateRange(String from, String to) {
        return new ItemViewException(ItemViewResponseCode.INVALID_DATE_RANGE,
                "from=" + from + ", to=" + to);
    }
}
