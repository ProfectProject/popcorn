package com.popcorn.demo.domain.qr.controller;

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
import com.popcorn.demo.global.exception.BaseException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class QrExceptionHandler extends BaseController {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBaseException(BaseException ex) {
		log.warn("QR exception: code={}, message={}", ex.getResponseCode().getCode(), ex.getMessage());
		return error(ex.getResponseCode(), ex.getMessage());
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
		log.error("QR unexpected error: {}", ex.getMessage(), ex);
		return error(CommonResponseCode.INTERNAL_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
	}
}
