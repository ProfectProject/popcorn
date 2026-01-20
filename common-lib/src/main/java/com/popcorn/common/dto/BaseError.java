package com.popcorn.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter

@NoArgsConstructor(access = AccessLevel.PROTECTED)

@AllArgsConstructor(access = AccessLevel.PRIVATE)

@Builder

public class BaseError {

	private int code;

	private String message;

	private String detail;



	public static BaseError from(ResponseCode responseCode) {

		return BaseError.builder()

				.code(responseCode.getCode())

				.message(responseCode.getMessage())

				.build();

	}



	public static BaseError of(ResponseCode responseCode, String detail) {

		return BaseError.builder()

				.code(responseCode.getCode())

				.message(responseCode.getMessage())

				.detail(detail)

				.build();

	}

}
