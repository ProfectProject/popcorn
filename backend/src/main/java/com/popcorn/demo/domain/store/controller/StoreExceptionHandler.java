package com.popcorn.demo.domain.store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.global.exception.BaseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "com.popcorn.demo.domain.store")
public class StoreExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleBaseException(BaseException ex) {
        log.warn("Store error: {}", ex.getMessage());
        return ResponseEntity.status(ex.getResponseCode().getHttpStatus())
                .body(BaseResponse.error(ex.getResponseCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Store validation error: {}", ex.getMessage());
        return createErrorResponse(CommonResponseCode.INVALID_REQUEST, "입력값이 유효하지 않습니다.");
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleBindException(BindException ex) {
        log.warn("Store bind error: {}", ex.getMessage());
        return createErrorResponse(CommonResponseCode.INVALID_REQUEST, "요청 데이터 바인딩에 실패했습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
        log.error("Unhandled store error: {}", ex.getMessage(), ex);
        return createErrorResponse(CommonResponseCode.INTERNAL_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<BaseResponse<BaseError>> createErrorResponse(CommonResponseCode code, String message) {
        BaseError error = BaseError.of(code, message);
        return ResponseEntity.status(code.getHttpStatus())
                .body(BaseResponse.error(error));
    }
}