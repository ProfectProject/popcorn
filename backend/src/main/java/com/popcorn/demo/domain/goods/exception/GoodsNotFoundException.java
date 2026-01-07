package com.popcorn.demo.domain.goods.exception;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class GoodsNotFoundException extends BaseException {
    public GoodsNotFoundException() {
        super(CommonResponseCode.NOT_FOUND);
    }
}
