package com.popcorn.demo.domain.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.common.exception.BaseException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;


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
@org.springframework.core.annotation.Order(10) // GlobalExceptionHandler 다음 순위
@Slf4j
public class OrderExceptionHandler extends BaseController {

	/**
	 * 공통 예외 처리 (BaseException 상속 예외들)
	 */
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBaseException(BaseException ex) {
		logBusinessException(ex);
		String userFriendlyMessage = getUserFriendlyMessage(ex);
		return error(ex.getResponseCode(), userFriendlyMessage);
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
		return error(CommonResponseCode.INVALID_REQUEST, message);
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
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}

	/**
	 * 필수 요청 파라미터 누락 (400 Bad Request)
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleMissingRequestParam(
			MissingServletRequestParameterException ex) {
		log.warn("Order missing request parameter: {}", ex.getMessage());
		String message = String.format("필수 요청 파라미터가 누락되었습니다: %s", ex.getParameterName());
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}

	/**
	 * 잘못된 타입 변환 오류 처리 (400 Bad Request)
	 * 예: 잘못된 UUID 형식
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
		String requiredTypeName = ex.getRequiredType() != null
				? ex.getRequiredType().getSimpleName()
				: "알 수 없는 타입";
		log.warn("Order parameter type mismatch: parameter={}, value={}, requiredType={}",
				ex.getName(), ex.getValue(), requiredTypeName);

		String message = String.format("잘못된 %s 형식입니다: %s", requiredTypeName, ex.getValue());
		return error(CommonResponseCode.INVALID_REQUEST, message);
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
		return error(CommonResponseCode.INVALID_REQUEST, userMessage);
	}

	/**
	 * 인증 자격 증명 누락 오류 (403 Forbidden)
	 */
	@ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleAuthenticationCredentialsNotFoundException(
			AuthenticationCredentialsNotFoundException ex) {
		log.warn("🔒 인증 자격 증명 누락: {}", ex.getMessage());
		String userMessage = "인증이 필요합니다.";
		return error(CommonResponseCode.FORBIDDEN, userMessage);
	}

	/**
	 * 잘못된 인증 정보 오류 (400 Bad Request)
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBadCredentialsException(
			BadCredentialsException ex) {
		log.warn("🔒 잘못된 인증 정보: {}", ex.getMessage());
		String userMessage = "아이디 또는 비밀번호가 잘못되었습니다.";
		return error(CommonResponseCode.INVALID_REQUEST, userMessage);
	}


	/**
	 * 기타 예상치 못한 예외 처리 (500 Internal Server Error)
	 * HttpRequestMethodNotSupportedException과 NoResourceFoundException은 제외 - Spring이 기본 상태로 처리하도록 함
	 * 인증 관련 예외는 별도 핸들러에서 처리하므로 제외
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex)
			throws HttpRequestMethodNotSupportedException, NoResourceFoundException {
		// HttpRequestMethodNotSupportedException은 Spring이 405 상태로 처리하도록 재throw
		if (ex instanceof HttpRequestMethodNotSupportedException) {
			throw (HttpRequestMethodNotSupportedException) ex;
		}

		// NoResourceFoundException은 Spring이 404 상태로 처리하도록 재throw
		if (ex instanceof NoResourceFoundException) {
			throw (NoResourceFoundException) ex;
		}

		// 인증 관련 예외는 이 클래스의 특화 핸들러에서 처리되어야 하는데 여기까지 온 경우
		// 명시적으로 해당 핸들러로 리다이렉트
		if (ex instanceof AuthenticationCredentialsNotFoundException) {
			return handleAuthenticationCredentialsNotFoundException((AuthenticationCredentialsNotFoundException) ex);
		}
		if (ex instanceof BadCredentialsException) {
			return handleBadCredentialsException((BadCredentialsException) ex);
		}

		log.error("🚨 주문 처리 중 예상치 못한 오류 발생 - 타입: {}, 메시지: {}",
			ex.getClass().getSimpleName(), ex.getMessage(), ex);

		String userMessage = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
		return error(CommonResponseCode.INTERNAL_ERROR, userMessage);
	}

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

	private String getUserFriendlyMessage(BaseException ex) {
		if (ex.getResponseCode() instanceof OrderResponseCode orderCode) {
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

		return switch (ex.getResponseCode().getHttpStatus()) {
			case 400 -> "요청하신 내용을 확인해 주세요.";
			case 403 -> "접근 권한이 없습니다.";
			case 404 -> "요청하신 정보를 찾을 수 없습니다.";
			case 409 -> "현재 상태에서는 해당 작업을 수행할 수 없습니다.";
			default -> ex.getMessage();
		};
	}

	private boolean isClientError(BaseException ex) {
		int httpStatus = ex.getResponseCode().getHttpStatus();
		return httpStatus >= 400 && httpStatus < 500;
	}

	private boolean isBusinessRuleViolation(BaseException ex) {
		return ex.getResponseCode().getHttpStatus() == 409;
	}
}
