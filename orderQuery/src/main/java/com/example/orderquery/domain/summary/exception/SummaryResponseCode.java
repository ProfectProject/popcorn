package com.example.orderquery.domain.summary.exception;

import com.popcorn.common.dto.ResponseCode;
import lombok.Getter;

@Getter
public enum SummaryResponseCode implements ResponseCode {

    SUMMARY_NOT_FOUND(2404, 404, "요약 정보를 찾을 수 없습니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    SummaryResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
