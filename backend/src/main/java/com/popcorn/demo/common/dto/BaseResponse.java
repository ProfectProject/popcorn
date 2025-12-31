package com.popcorn.demo.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter

@NoArgsConstructor(access = AccessLevel.PROTECTED)

@AllArgsConstructor(access = AccessLevel.PRIVATE)

@Builder

public class BaseResponse<T> {

	private int code;

	private String message;

	private T data;



	/**
	 * 공통 응답 포맷 생성
	 */

	public static <T> BaseResponse<T> of(int code, String message, T data) {
		return BaseResponse.<T>builder()

				.code(code)

				.message(message)

				.data(data)

				.build();

	}



	public static <T> BaseResponse<T> from(ResponseCode responseCode, T data) {

		return of(responseCode.getCode(), responseCode.getMessage(), data);

	}



	public static <T> BaseResponse<T> success(T data) {

		return from(ResponseCode.SUCCESS, data);

	}



	public static BaseResponse<Void> error(ResponseCode responseCode) {

		return from(responseCode, null);

	}

}
