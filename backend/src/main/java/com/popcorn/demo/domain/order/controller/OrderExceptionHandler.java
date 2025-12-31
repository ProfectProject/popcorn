package com.popcorn.demo.domain.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;

/**

	* 주문 관련 예외를 전역적으로 처리하는 핸들러

	*

	* 처리 예외 유형:

	* - OrderValidationException: 400 Bad Request

	* - OrderNotFoundException: 404 Not Found

	* - OrderConflictException: 409 Conflict

	* - MethodArgumentNotValidException: 400 Bad Request (Validation 오류)

	* - 기타 예상치 못한 예외: 500 Internal Server Error

	*/

@RestControllerAdvice(basePackages = "com.popcorn.demo.domain.order")

public class OrderExceptionHandler {



	/**

		* 공통 예외 처리 (BaseException 상속 예외들)

		*/

	@ExceptionHandler(BaseException.class)

	public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException ex) {

		BaseResponse<Void> response = BaseResponse.error(ex.getResponseCode());

		return ResponseEntity.status(ex.getResponseCode().getHttpStatus()).body(response);

	}



	/**

		* Bean Validation 오류 처리 (400 Bad Request)

		*/

	@ExceptionHandler(MethodArgumentNotValidException.class)

	public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult().getFieldErrors().stream()

				.map(error -> error.getField() + ": " + error.getDefaultMessage())

				.findFirst()

				.orElse("입력값이 올바르지 않습니다.");



		BaseResponse<Void> response = BaseResponse.of(

				ResponseCode.INVALID_REQUEST.getCode(),

				message,

				null

		);

		return ResponseEntity.status(ResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);

	}



	/**

		* 기타 예상치 못한 예외 처리 (500 Internal Server Error)

		*/

	@ExceptionHandler(Exception.class)

	public ResponseEntity<BaseResponse<Void>> handleGeneralException(Exception ex) {

		BaseResponse<Void> response = BaseResponse.error(ResponseCode.INTERNAL_ERROR);

		return ResponseEntity.status(ResponseCode.INTERNAL_ERROR.getHttpStatus()).body(response);

	}

}
