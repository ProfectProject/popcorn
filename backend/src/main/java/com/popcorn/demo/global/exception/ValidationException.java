package com.popcorn.demo.global.exception;

/**
 * 입력 값 검증 실패 시 발생하는 예외
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}