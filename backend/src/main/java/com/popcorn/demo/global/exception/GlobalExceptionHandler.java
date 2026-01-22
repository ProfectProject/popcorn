package com.popcorn.demo.global.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
@Order(1) // 인증 관련 예외를 우선 처리하기 위해 높은 우선순위
@Slf4j
public class GlobalExceptionHandler extends BaseController {

    /**
     * 입력 값 검증 실패 (@Valid 어노테이션)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("code", 400);
        response.put("message", "입력 값 검증 실패");
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 사용자 정의 검증 실패 (ValidationException)
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("code", 400);
        response.put("message", ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
     * 기타 RuntimeException (기존 호환성 유지)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();

        // 자세한 로그 출력
        log.error("🚨 RuntimeException 발생 - 타입: {}, 메시지: {}",
            ex.getClass().getSimpleName(), ex.getMessage(), ex);

        // 사용자 관련 검증 오류는 400으로 처리
        if (ex.getMessage() != null &&
            (ex.getMessage().contains("비밀번호가 일치하지 않습니다") ||
             ex.getMessage().contains("Email already exists") ||
             ex.getMessage().contains("Phone number already exists"))) {
            response.put("code", 400);
            response.put("message", ex.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // 기타 RuntimeException은 500으로 처리
        response.put("code", 500);
        response.put("message", "서버 내부 오류가 발생했습니다.");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}