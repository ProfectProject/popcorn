package com.popcorn.order.exception;

import lombok.Getter;

/**
 * 멱등성 키 관련 오류가 발생했을 때 던지는 예외
 *
 * 중복된 요청, 유효하지 않은 키, 만료된 키 등
 * 멱등성 처리 과정에서 발생하는 모든 문제를 포괄합니다.
 */
@Getter
public class IdempotencyKeyException extends RuntimeException {

    private final String key;
    private final String errorType;

    public IdempotencyKeyException(String key, String message) {
        super(message);
        this.key = key;
        this.errorType = "INVALID_KEY";
    }

    public IdempotencyKeyException(String key, String errorType, String message) {
        super(message);
        this.key = key;
        this.errorType = errorType;
    }

    public IdempotencyKeyException(String key, String message, Throwable cause) {
        super(message, cause);
        this.key = key;
        this.errorType = "PROCESSING_ERROR";
    }

    /**
     * 중복 요청 감지시 사용하는 팩토리 메서드
     */
    public static IdempotencyKeyException duplicateRequest(String key) {
        return new IdempotencyKeyException(key, "DUPLICATE_REQUEST",
                "이미 처리된 요청입니다. 중복 요청이 감지되었습니다.");
    }

    /**
     * 만료된 키 사용시 사용하는 팩토리 메서드
     */
    public static IdempotencyKeyException expiredKey(String key) {
        return new IdempotencyKeyException(key, "EXPIRED_KEY",
                "만료된 멱등성 키입니다. 새로운 키를 발급받아 주세요.");
    }

    /**
     * 유효하지 않은 키 형식시 사용하는 팩토리 메서드
     */
    public static IdempotencyKeyException invalidFormat(String key) {
        return new IdempotencyKeyException(key, "INVALID_FORMAT",
                "멱등성 키 형식이 올바르지 않습니다.");
    }

}