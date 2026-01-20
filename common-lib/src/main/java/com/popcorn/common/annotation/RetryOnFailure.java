package com.popcorn.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 실패 시 재시도를 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 실패 시 자동으로 재시도합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @RetryOnFailure(
 *     maxAttempts = 3,
 *     backoffMillis = 1000,
 *     exponentialBackoff = true,
 *     retryOn = {RuntimeException.class}
 * )
 * public PaymentResponse processPayment(PaymentRequest request) {
 *     // 결제 처리 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryOnFailure {

    /**
     * 최대 시도 횟수 (첫 번째 시도 포함)
     */
    int maxAttempts() default 3;

    /**
     * 재시도 간 대기 시간 (밀리초)
     */
    long backoffMillis() default 1000;

    /**
     * 지수 백오프 사용 여부
     * true인 경우: 1초 -> 2초 -> 4초 -> 8초 ...
     */
    boolean exponentialBackoff() default false;

    /**
     * 지수 백오프 배수
     */
    double multiplier() default 2.0;

    /**
     * 최대 대기 시간 (밀리초)
     */
    long maxBackoffMillis() default 30000;

    /**
     * 재시도할 예외 타입들
     */
    Class<? extends Throwable>[] retryOn() default {RuntimeException.class};

    /**
     * 재시도하지 않을 예외 타입들
     */
    Class<? extends Throwable>[] noRetryOn() default {};

    /**
     * 재시도 조건 SpEL 표현식
     */
    String retryCondition() default "";

    /**
     * 재시도 불가 조건 SpEL 표현식
     */
    String noRetryCondition() default "";

    /**
     * 재시도 실패 시 폴백 메서드 이름
     */
    String fallbackMethod() default "";

    /**
     * 재시도 과정에서 발생하는 이벤트 로깅 여부
     */
    boolean logRetryAttempts() default true;

    /**
     * 재시도 통계 수집 여부
     */
    boolean collectMetrics() default true;

    /**
     * 커스텀 재시도 키 (메트릭 구분용)
     */
    String retryKey() default "";
}