package com.popcorn.demo.domain.popup.controller;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseError;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.popup.exception.PopupException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Order(1) // GlobalExceptionHandler보다 높은 우선순위
@Slf4j
public class PopupExceptionHandler extends BaseController {

	@ExceptionHandler(PopupException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleBaseException(PopupException ex) {
		logBusinessException(ex);
		String message = getUserFriendlyMessage(ex);
		return error(ex.getResponseCode(), message);
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
		String message = String.format("잘못된 %s 형식입니다: %s",
				ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "요청",
				ex.getValue());
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}


	private void logBusinessException(PopupException ex) {
		if (ex.getResponseCode().getHttpStatus() >= 500) {
			log.error("🚨 팝업 서버 오류 - 코드: {}, 메시지: {}", ex.getResponseCode().getCode(), ex.getMessage(), ex);
		} else {
			log.warn("⚠️ 팝업 요청 오류 - 코드: {}, 메시지: {}", ex.getResponseCode().getCode(), ex.getMessage());
		}
	}

	private String getUserFriendlyMessage(PopupException ex) {
		if (ex.getResponseCode() == com.popcorn.demo.domain.popup.dto.PopupResponseCode.POPUP_NOT_FOUND) {
			return "팝업 정보를 찾을 수 없습니다.";
		}
		if (ex.getResponseCode() == com.popcorn.demo.domain.popup.dto.PopupResponseCode.INVALID_REQUEST) {
			return "요청을 확인해 주세요.";
		}
		return ex.getMessage();
	}
}
