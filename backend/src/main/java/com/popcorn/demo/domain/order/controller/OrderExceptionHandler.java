package com.popcorn.demo.domain.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;

import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.global.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

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

@RestControllerAdvice
@Slf4j
public class OrderExceptionHandler {



	/**

		* 공통 예외 처리 (BaseException 상속 예외들)

		*/

	@ExceptionHandler(BaseException.class)

	public ResponseEntity<BaseResponse<BaseError>> handleBaseException(BaseException ex) {
		// 상태 전이 에러인 경우 더 상세한 로그 출력
		if (ex.getResponseCode() == OrderResponseCode.INVALID_STATUS_TRANSITION) {
			log.warn("❌ 주문 상태 전이 실패: code={}, message={}, cause={}",
				ex.getResponseCode(), ex.getMessage(), ex.getCause() != null ? ex.getCause().getMessage() : "N/A");
		} else {
			log.warn("Order error handled: code={}, message={}", ex.getResponseCode(), ex.getMessage());
		}

		BaseResponse<BaseError> response = BaseResponse.error(ex.getResponseCode(), ex.getMessage());

		return ResponseEntity.status(ex.getResponseCode().getHttpStatus()).body(response);

	}



	/**

		* Bean Validation 오류 처리 (400 Bad Request)

		*/

	@ExceptionHandler(MethodArgumentNotValidException.class)

	public ResponseEntity<BaseResponse<BaseError>> handleValidationException(MethodArgumentNotValidException ex) {
		log.warn("Order validation error: {}", ex.getMessage());

		String message = ex.getBindingResult().getFieldErrors().stream()

				.map(error -> error.getField() + ": " + error.getDefaultMessage())

				.findFirst()

				.orElse("입력값이 올바르지 않습니다.");



		BaseError error = BaseError.of(CommonResponseCode.INVALID_REQUEST, message);
		BaseResponse<BaseError> response = BaseResponse.error(error);
		return ResponseEntity.status(CommonResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);

	}

	/**
		* Binding 오류 처리 (400 Bad Request)
		*/
	@ExceptionHandler(BindException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBindException(BindException ex) {
		log.warn("Order validation error (Bind): {}", ex.getMessage());
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.findFirst()
				.orElse("입력값이 올바르지 않습니다.");

		BaseError error = BaseError.of(CommonResponseCode.INVALID_REQUEST, message);
		BaseResponse<BaseError> response = BaseResponse.error(error);
		return ResponseEntity.status(CommonResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);
	}

	/**
	 * 잘못된 타입 변환 오류 처리 (400 Bad Request)
	 * 예: 잘못된 UUID 형식
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
		log.warn("Order parameter type mismatch: parameter={}, value={}, requiredType={}",
				ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());

		String message = String.format("잘못된 %s 형식입니다: %s", ex.getRequiredType().getSimpleName(), ex.getValue());

		BaseError error = BaseError.of(CommonResponseCode.INVALID_REQUEST, message);
		BaseResponse<BaseError> response = BaseResponse.error(error);
		return ResponseEntity.status(CommonResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);
	}



	/**

		* 기타 예상치 못한 예외 처리 (500 Internal Server Error)

		*/

	@ExceptionHandler(Exception.class)

	public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
		log.error("Unhandled order error", ex);

		BaseResponse<BaseError> response = BaseResponse.error(CommonResponseCode.INTERNAL_ERROR, ex.getMessage());

		return ResponseEntity.status(CommonResponseCode.INTERNAL_ERROR.getHttpStatus()).body(response);

	}

}
