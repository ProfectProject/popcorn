package com.popcorn.order.exception;

import java.util.UUID;

import lombok.Getter;

/**
 * 결제 처리 중 오류가 발생했을 때 던지는 예외
 *
 * 결제 서비스 연동 실패, 카드 오류, 잔액 부족 등
 * 결제 관련 모든 문제를 포괄하는 예외입니다.
 */
@Getter
public class PaymentProcessingException extends RuntimeException {

    private final UUID orderId;
    private final String paymentMethod;
    private final String paymentErrorCode;

    public PaymentProcessingException(UUID orderId, String paymentMethod, String message) {
        super(message);
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.paymentErrorCode = null;
    }

    public PaymentProcessingException(UUID orderId, String paymentMethod, String paymentErrorCode, String message) {
        super(message);
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.paymentErrorCode = paymentErrorCode;
    }

    public PaymentProcessingException(UUID orderId, String paymentMethod, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.paymentErrorCode = null;
    }

}