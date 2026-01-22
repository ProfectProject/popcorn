package com.popcorn.demo.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.common.exception.BaseException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class PaymentExceptionHandler extends BaseController {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBaseException(BaseException ex) {
		log.warn("Payment exception: code={}, message={}", ex.getResponseCode().getCode(), ex.getMessage());
		return error(ex.getResponseCode(), getUserFriendlyMessage(ex));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleValidationException(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.findFirst()
				.orElse("입력값이 올바르지 않습니다.");
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBindException(BindException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.findFirst()
				.orElse("입력값이 올바르지 않습니다.");
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
		String requiredTypeName = ex.getRequiredType() != null
				? ex.getRequiredType().getSimpleName()
				: "알 수 없는 타입";
		String message = String.format("잘못된 %s 형식입니다: %s", requiredTypeName, ex.getValue());
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
		log.error("Payment unexpected error: {}", ex.getMessage(), ex);
		return error(CommonResponseCode.INTERNAL_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
	}

	private String getUserFriendlyMessage(BaseException ex) {
		if (ex.getResponseCode() == CommonResponseCode.NOT_FOUND) {
			return "결제 정보를 찾을 수 없습니다.";
		}
		if (ex.getResponseCode() == CommonResponseCode.INVALID_REQUEST) {
			return "요청값을 확인해 주세요.";
		}
		if (ex.getResponseCode() instanceof OrderResponseCode orderCode) {
			return switch (orderCode) {
				case PAYMENT_ALREADY_EXISTS -> "이미 결제 기록이 존재합니다.";
				case INVALID_STATUS_TRANSITION -> "현재 상태에서는 해당 작업을 수행할 수 없습니다.";
				default -> ex.getMessage();
			};
		}
		return ex.getMessage();
	}
}
