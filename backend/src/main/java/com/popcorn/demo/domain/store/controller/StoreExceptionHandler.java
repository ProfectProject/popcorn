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
        log.warn("[STORE_BUSINESS_ERROR] code={}, message={}", ex.getResponseCode().name(), ex.getMessage());
        
        BaseResponse<BaseError> response = BaseResponse.error(ex.getResponseCode(), ex.getMessage());
        return ResponseEntity.status(ex.getResponseCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("[STORE_VALIDATION_ERROR] 입력값 유효성 검증 실패: {}", ex.getMessage());
        
        String message = "입력값이 유효하지 않습니다.";
        BaseError error = BaseError.of(CommonResponseCode.INVALID_REQUEST, message);
        BaseResponse<BaseError> response = BaseResponse.error(error);
        
        return ResponseEntity.status(CommonResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleBindException(BindException ex) {
        log.warn("[STORE_BIND_ERROR] 데이터 바인딩 실패: {}", ex.getMessage());
        
        String message = "요청 데이터 바인딩에 실패했습니다.";
        BaseError error = BaseError.of(CommonResponseCode.INVALID_REQUEST, message);
        BaseResponse<BaseError> response = BaseResponse.error(error);
        
        return ResponseEntity.status(CommonResponseCode.INVALID_REQUEST.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<BaseError>> handleGeneralException(Exception ex) {
        log.error("[STORE_SYSTEM_ERROR] 예상치 못한 시스템 오류 발생: {}", ex.getMessage(), ex);
        
        BaseResponse<BaseError> response = BaseResponse.error(CommonResponseCode.INTERNAL_ERROR, "서버 내부 오류가 발생했습니다.");
        
        return ResponseEntity.status(CommonResponseCode.INTERNAL_ERROR.getHttpStatus()).body(response);
    }
}