package com.example.orderquery.global.exception;

import com.popcorn.common.dto.ResponseCode;
import com.popcorn.common.exception.BaseException;

public class OwnerAuthException extends BaseException {

    private final String detail;

    public OwnerAuthException(ResponseCode responseCode, String detail) {
        super(responseCode, detail);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
