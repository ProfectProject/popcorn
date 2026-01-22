package com.popcorn.store.domain.goods.dto;

import com.popcorn.common.dto.ResponseCode;

import lombok.Getter;

@Getter
public enum GoodsResponseCode implements ResponseCode {

	GOODS_NOT_FOUND(2201, 404, "굿즈 정보를 찾을 수 없습니다."),
	INVALID_QUANTITY(2202, 400, "수량이 올바르지 않습니다."),
	INSUFFICIENT_STOCK(2203, 400, "재고가 부족합니다.");

	private final int code;
	private final int httpStatus;
	private final String message;

	GoodsResponseCode(int code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
