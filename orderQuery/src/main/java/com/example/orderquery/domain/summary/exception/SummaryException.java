package com.example.orderquery.domain.summary.exception;

import java.util.UUID;

import com.popcorn.common.exception.BaseException;

public class SummaryException extends BaseException {

    private final String detail;

    private SummaryException(SummaryResponseCode responseCode, String detail) {
        super(responseCode, detail);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    public static SummaryException notFound(UUID storeId, UUID popupId) {
        return new SummaryException(SummaryResponseCode.SUMMARY_NOT_FOUND,
                "storeId=" + storeId + ", popupId=" + popupId);
    }
}
