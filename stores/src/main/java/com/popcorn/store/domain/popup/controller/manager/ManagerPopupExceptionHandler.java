package com.popcorn.store.domain.popup.controller.manager;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseError;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.dto.CommonResponseCode;
import com.popcorn.store.domain.popup.exception.manager.ManagerPopupException;
import com.popcorn.store.domain.store.exception.StoreException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice(basePackageClasses = ManagerPopupController.class)
@Order(0)
@Slf4j
public class ManagerPopupExceptionHandler extends BaseController {

    @ExceptionHandler(ManagerPopupException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleManagerPopupException(ManagerPopupException ex) {
        logBusinessException(ex);
        return error(ex.getResponseCode(), ex.getMessage());
    }

    @ExceptionHandler(StoreException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleStoreException(StoreException ex) {
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("⚠️ 매니저 팝업 요청 본문 파싱 오류: {}", ex.getMessage());
        return error(CommonResponseCode.INVALID_REQUEST, "요청 본문 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("⚠️ 매니저 권한 거부: {}", ex.getMessage());
        return error(CommonResponseCode.FORBIDDEN, "권한이 없습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
        log.error("🚨 매니저 팝업 처리 중 오류 발생: {}", ex.getMessage(), ex);
        return error(CommonResponseCode.INTERNAL_ERROR, "일시적인 오류가 발생했습니다.");
    }

    private void logBusinessException(Exception ex) {
        if (ex instanceof ManagerPopupException managerPopupException) {
            if (managerPopupException.getResponseCode().getHttpStatus() >= 500) {
                log.error("🚨 매니저 팝업 서버 오류 - 코드: {}, 메시지: {}", managerPopupException.getResponseCode().getCode(),
                        managerPopupException.getMessage(), managerPopupException);
            } else {
                log.warn("⚠️ 매니저 팝업 요청 오류 - 코드: {}, 메시지: {}", managerPopupException.getResponseCode().getCode(),
                        managerPopupException.getMessage());
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
        }
    }
}
