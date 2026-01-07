package com.popcorn.demo.domain.popup.controller.owner;

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
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.exception.owner.OwnerPopupException;
import com.popcorn.demo.domain.store.exception.StoreException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice(basePackages = "com.popcorn.demo.domain.popup.controller.owner")
@Slf4j
public class OwnerPopupExceptionHandler extends BaseController {

	@ExceptionHandler(PopupException.class)
	public ResponseEntity<BaseResponse<BaseError>> handlePopupException(PopupException ex) {
		logBusinessException(ex);
		return error(ex.getResponseCode(), ex.getMessage());
	}

	@ExceptionHandler(StoreException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleStoreException(StoreException ex) {
		logBusinessException(ex);
		return error(ex.getResponseCode(), ex.getMessage());
	}

	@ExceptionHandler(OwnerPopupException.class)
	public ResponseEntity<BaseResponse<BaseError>> handleOwnerPopupException(OwnerPopupException ex) {
		logBusinessException(ex);
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
		String message = String.format("잘못된 %s 형식입니다: %s",
				ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "요청",
				ex.getValue());
		return error(CommonResponseCode.INVALID_REQUEST, message);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
		log.error("🚨 오너 팝업 처리 중 오류 발생 - 타입: {}, 메시지: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
		return error(CommonResponseCode.INTERNAL_ERROR, "일시적인 오류가 발생했습니다.");
	}

	private void logBusinessException(Exception ex) {
		if (ex instanceof PopupException popupException) {
			if (popupException.getResponseCode().getHttpStatus() >= 500) {
				log.error("🚨 팝업 서버 오류 - 코드: {}, 메시지: {}", popupException.getResponseCode().getCode(),
						popupException.getMessage(), popupException);
			} else {
				log.warn("⚠️ 팝업 요청 오류 - 코드: {}, 메시지: {}", popupException.getResponseCode().getCode(),
						popupException.getMessage());
			}
			return;
		}
		if (ex instanceof StoreException storeException) {
			if (storeException.getResponseCode().getHttpStatus() >= 500) {
				log.error("🚨 스토어 서버 오류 - 코드: {}, 메시지: {}", storeException.getResponseCode().getCode(),
						storeException.getMessage(), storeException);
			} else {
				log.warn("⚠️ 스토어 요청 오류 - 코드: {}, 메시지: {}", storeException.getResponseCode().getCode(),
						storeException.getMessage());
			}
			return;
		}
		if (ex instanceof OwnerPopupException ownerPopupException) {
			if (ownerPopupException.getResponseCode().getHttpStatus() >= 500) {
				log.error("🚨 오너 팝업 서버 오류 - 코드: {}, 메시지: {}", ownerPopupException.getResponseCode().getCode(),
						ownerPopupException.getMessage(), ownerPopupException);
			} else {
				log.warn("⚠️ 오너 팝업 요청 오류 - 코드: {}, 메시지: {}", ownerPopupException.getResponseCode().getCode(),
						ownerPopupException.getMessage());
			}
		}
	}
}
