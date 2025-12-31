package com.popcorn.demo.common.controller;

import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.ResponseCode;

public abstract class BaseController {

	protected <T> ResponseEntity<BaseResponse<T>> ok(T data) {

		return ResponseEntity.ok(BaseResponse.success(data));

	}



	protected ResponseEntity<BaseResponse<Void>> error(ResponseCode responseCode) {

		return ResponseEntity.status(responseCode.getHttpStatus())
				.body(BaseResponse.error(responseCode));

	}

}
