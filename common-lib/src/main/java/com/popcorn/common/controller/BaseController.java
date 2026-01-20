package com.popcorn.common.controller;

import org.springframework.http.ResponseEntity;

import com.popcorn.common.dto.BaseError;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.dto.ResponseCode;

public abstract class BaseController {

	protected <T> ResponseEntity<BaseResponse<T>> ok(T data) {

		return ResponseEntity.ok(BaseResponse.success(data));

	}

	protected <T> ResponseEntity<BaseResponse<T>> accepted(T data) {

		return ResponseEntity.accepted().body(BaseResponse.success(data));

	}

	protected ResponseEntity<Void> noContent() {

		return ResponseEntity.noContent().build();

	}

	protected ResponseEntity<String> okText(String body) {

		return ResponseEntity.ok(body);

	}

	protected <T> ResponseEntity<BaseResponse<T>> created(T data) {

		return ResponseEntity.status(201).body(BaseResponse.success(data));

	}



	protected ResponseEntity<BaseResponse<Void>> error(ResponseCode responseCode) {

		return ResponseEntity.status(responseCode.getHttpStatus())
				.body(BaseResponse.error(responseCode));

	}

	protected ResponseEntity<BaseResponse<BaseError>> error(ResponseCode responseCode, String detail) {

		return ResponseEntity.status(responseCode.getHttpStatus())
				.body(BaseResponse.error(responseCode, detail));

	}

}
