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
import com.popcorn.demo.common.cache.IdempotencyService;
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
		// 예외 타입별 세분화된 로깅
		logBusinessException(ex);

		// 사용자 친화적 메시지 변환
		String userFriendlyMessage = getUserFriendlyMessage(ex);

		BaseResponse<BaseError> response = BaseResponse.error(ex.getResponseCode(), userFriendlyMessage);
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
	 * 멱등성 처리 오류 (409 Conflict)
	 */
	@ExceptionHandler(IdempotencyService.IdempotencyException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleIdempotencyException(IdempotencyService.IdempotencyException ex) {
		if (ex.getIdempotencyKey() != null) {
			log.warn("🔄 멱등성 처리 중 오류 - 키: {}, 메시지: {}", ex.getIdempotencyKey(), ex.getMessage());
		} else {
			log.warn("🔄 멱등성 처리 중 오류 - 메시지: {}", ex.getMessage());
		}

		String userMessage = "동일한 요청이 이미 처리되고 있거나 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
		BaseError error = BaseError.of(CommonResponseCode.INVALID_REQUEST, userMessage);
		BaseResponse<BaseError> response = BaseResponse.error(error);
		return ResponseEntity.status(CommonResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);
	}



	/**

		* 기타 예상치 못한 예외 처리 (500 Internal Server Error)

		*/

	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
		log.error("🚨 주문 처리 중 예상치 못한 오류 발생 - 타입: {}, 메시지: {}",
			ex.getClass().getSimpleName(), ex.getMessage(), ex);

		String userMessage = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
		BaseResponse<BaseError> response = BaseResponse.error(CommonResponseCode.INTERNAL_ERROR, userMessage);

		return ResponseEntity.status(CommonResponseCode.INTERNAL_ERROR.getHttpStatus()).body(response);
	}

	// ================ 개선된 헬퍼 메서드들 ================

	/**
	 * 비즈니스 예외에 대한 세분화된 로깅
	 */
	private void logBusinessException(BaseException ex) {
		if (ex.getResponseCode() == OrderResponseCode.INVALID_STATUS_TRANSITION) {
			log.warn("❌ 주문 상태 전이 실패 - 코드: {}, 메시지: {}, 원인: {}",
				ex.getResponseCode().getCode(), ex.getMessage(),
				ex.getCause() != null ? ex.getCause().getMessage() : "N/A");
		} else if (isClientError(ex)) {
			log.warn("🔍 주문 클라이언트 오류 - 코드: {}, 메시지: {}",
				ex.getResponseCode().getCode(), ex.getMessage());
		} else if (isBusinessRuleViolation(ex)) {
			log.info("📋 주문 비즈니스 규칙 위반 - 코드: {}, 메시지: {}",
				ex.getResponseCode().getCode(), ex.getMessage());
		} else {
			log.error("🚨 주문 시스템 오류 - 코드: {}, 메시지: {}",
				ex.getResponseCode().getCode(), ex.getMessage(), ex);
		}
	}

	/**
	 * 사용자 친화적 메시지 변환
	 */
	private String getUserFriendlyMessage(BaseException ex) {
		// OrderResponseCode 기반 사용자 친화적 메시지 변환
		if (ex.getResponseCode() instanceof OrderResponseCode) {
			OrderResponseCode orderCode = (OrderResponseCode) ex.getResponseCode();
			return switch (orderCode) {
				case EMPTY_ITEMS -> "주문 항목을 추가해 주세요.";
				case INVALID_QTY -> "주문 수량을 확인해 주세요.";
				case PRODUCT_NOT_FOUND -> "선택하신 상품을 찾을 수 없습니다.";
				case OUT_OF_STOCK -> "죄송합니다. 재고가 부족합니다.";
				case INVALID_STATUS_TRANSITION -> "현재 상태에서는 해당 작업을 수행할 수 없습니다.";
				case ALREADY_CANCELED -> "이미 취소된 주문입니다.";
				default -> ex.getMessage();
			};
		}

		// 기본 공통 응답 코드의 경우
		return switch (ex.getResponseCode().getHttpStatus()) {
			case 400 -> "요청하신 내용을 확인해 주세요.";
			case 403 -> "접근 권한이 없습니다.";
			case 404 -> "요청하신 정보를 찾을 수 없습니다.";
			case 409 -> "현재 상태에서는 해당 작업을 수행할 수 없습니다.";
			default -> ex.getMessage();
		};
	}

	/**
	 * 클라이언트 오류 여부 판단 (4xx)
	 */
	private boolean isClientError(BaseException ex) {
		int httpStatus = ex.getResponseCode().getHttpStatus();
		return httpStatus >= 400 && httpStatus < 500;
	}

	/**
	 * 비즈니스 규칙 위반 여부 판단 (409 Conflict)
	 */
	private boolean isBusinessRuleViolation(BaseException ex) {
		return ex.getResponseCode().getHttpStatus() == 409;
	}

}
