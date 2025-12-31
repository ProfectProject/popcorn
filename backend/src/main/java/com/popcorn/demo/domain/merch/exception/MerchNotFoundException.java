package com.popcorn.demo.domain.merch.exception;

import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;

public class MerchNotFoundException extends BaseException {
    public MerchNotFoundException() {
        super(ResponseCode.NOT_FOUND);
    }
}
