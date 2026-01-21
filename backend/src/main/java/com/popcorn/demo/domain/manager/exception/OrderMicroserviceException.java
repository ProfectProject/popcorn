package com.popcorn.demo.domain.manager.exception;

/**
 * Order 마이크로서비스 호출 시 발생하는 예외
 */
public class OrderMicroserviceException extends RuntimeException {

    public OrderMicroserviceException(String message) {
        super(message);
    }

    public OrderMicroserviceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Circuit Breaker가 열린 상태일 때 발생하는 예외
     */
    public static class CircuitBreakerOpenException extends OrderMicroserviceException {
        public CircuitBreakerOpenException(String serviceName) {
            super("Order 마이크로서비스(" + serviceName + ")가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 마이크로서비스 호출 시 타임아웃 발생
     */
    public static class TimeoutException extends OrderMicroserviceException {
        public TimeoutException(String operation) {
            super("Order 마이크로서비스 " + operation + " 요청이 시간 초과되었습니다.");
        }
    }

    /**
     * 마이크로서비스에서 비즈니스 오류 응답
     */
    public static class BusinessException extends OrderMicroserviceException {
        public BusinessException(String message) {
            super(message);
        }
    }

}