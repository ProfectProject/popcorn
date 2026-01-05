package com.popcorn.demo.domain.goods.exception;

import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;

public class GoodsNotFoundException extends BaseException {
    public GoodsNotFoundException() {
        super(ResponseCode.NOT_FOUND);
    }
}
