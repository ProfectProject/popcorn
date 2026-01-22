package com.popcorn.common.exception;

import com.popcorn.common.dto.ResponseCode;

import lombok.Getter;

@Getter

public class BaseException extends RuntimeException {

	private final ResponseCode responseCode;



	public BaseException(ResponseCode responseCode, String s) {

		super(responseCode.getMessage());

		this.responseCode = responseCode;

	}



	public BaseException(ResponseCode responseCode, Throwable cause) {

		super(responseCode.getMessage(), cause);

		this.responseCode = responseCode;

	}

}
