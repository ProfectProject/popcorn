package com.popcorn.demo.global.exception;

import com.popcorn.demo.common.dto.ResponseCode;

import lombok.Getter;

@Getter

public class BaseException extends RuntimeException {

	private final ResponseCode responseCode;



	public BaseException(ResponseCode responseCode) {

		super(responseCode.getMessage());

		this.responseCode = responseCode;

	}



	public BaseException(ResponseCode responseCode, Throwable cause) {

		super(responseCode.getMessage(), cause);

		this.responseCode = responseCode;

	}

}

