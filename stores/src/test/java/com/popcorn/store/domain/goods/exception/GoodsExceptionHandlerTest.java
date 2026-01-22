package com.popcorn.store.domain.goods.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.popcorn.common.dto.BaseResponse;
import com.popcorn.store.domain.goods.dto.GoodsResponseCode;

class GoodsExceptionHandlerTest {

	@Test
	void handlesBaseException() {
		GoodsExceptionHandler handler = new GoodsExceptionHandler();

		ResponseEntity<BaseResponse<Void>> response = handler.handleBaseException(GoodsException.goodsNotFound());

		assertThat(response.getStatusCode().value())
				.isEqualTo(GoodsResponseCode.GOODS_NOT_FOUND.getHttpStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode())
				.isEqualTo(GoodsResponseCode.GOODS_NOT_FOUND.getCode());
	}
}
