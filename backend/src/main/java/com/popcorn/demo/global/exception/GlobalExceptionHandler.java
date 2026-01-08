package com.popcorn.demo.global.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
@Order(100) // 도메인별 ExceptionHandler보다 낮은 우선순위
@Slf4j
public class GlobalExceptionHandler {

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