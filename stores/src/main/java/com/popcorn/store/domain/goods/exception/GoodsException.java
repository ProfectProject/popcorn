package com.popcorn.store.domain.goods.exception;

import com.popcorn.common.exception.BaseException;
import com.popcorn.store.domain.goods.dto.GoodsResponseCode;

public class GoodsException extends BaseException {

	public GoodsException(GoodsResponseCode responseCode) {
		super(responseCode, responseCode.getMessage());
	}

	public static GoodsException goodsNotFound() {
		return new GoodsException(GoodsResponseCode.GOODS_NOT_FOUND);
	}

	public static GoodsException invalidQuantity() {
		return new GoodsException(GoodsResponseCode.INVALID_QUANTITY);
	}

	public static GoodsException insufficientStock() {
		return new GoodsException(GoodsResponseCode.INSUFFICIENT_STOCK);
	}
}
