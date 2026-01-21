package com.popcorn.order.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 재시도 로직 어노테이션
 *
 * 메서드 실행 실패 시 자동으로 재시도를 수행합니다.
 * 네트워크 문제, 일시적 서버 오류, 외부 API 호출 실패 등에 대응합니다.
 *
 * 사용 예시:
 * {@literal @}Retry(maxAttempts = 3, delay = 1000)
 * public PaymentResult processPayment(PaymentRequest request) { ... }
 *
 * {@literal @}Retry(maxAttempts = 5, delay = 500, backoffMultiplier = 2.0,
 *         retryFor = {IOException.class, TimeoutException.class})
 * public String callExternalApi(String request) { ... }
 *
 * 재시도 전략:
 * 1. Fixed Delay: 고정된 지연 시간
 * 2. Exponential Backoff: 지수적 증가하는 지연 시간
 * 3. Random Jitter: 임의의 변동을 추가한 지연 시간
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {

    /**
     * 최대 재시도 횟수 (첫 번째 실행 포함)
     * 기본값: 3
     */
    int maxAttempts() default 3;

    /**
     * 재시도 간격 (밀리초)
     * 기본값: 1000 (1초)
     */
    long delay() default 1000;

    /**
     * 백오프 배수 (지수적 증가 계수)
     * 1.0이면 고정 지연, 1.0보다 크면 지수적 증가
     * 기본값: 1.0 (고정 지연)
     */
    double backoffMultiplier() default 1.0;

    /**
     * 최대 지연 시간 (밀리초)
     * 백오프 사용 시 지연 시간이 이 값을 초과하지 않도록 제한
     * 기본값: 30000 (30초)
     */
    long maxDelay() default 30000;

    /**
     * 재시도 대상 예외 클래스 목록
     * 지정된 예외가 발생했을 때만 재시도
     * 기본값: {} (모든 예외)
     */
    Class<? extends Throwable>[] retryFor() default {};

    /**
     * 재시도 제외 예외 클래스 목록
     * 지정된 예외가 발생하면 재시도하지 않음
     * 기본값: {}
     */
    Class<? extends Throwable>[] noRetryFor() default {};

    /**
     * 랜덤 지터 추가 여부
     * true면 지연 시간에 ±25% 범위의 랜덤 변동 추가
     * 동시에 많은 재시도가 발생할 때 부하 분산 효과
     * 기본값: false
     */
    boolean jitter() default false;

    /**
     * 재시도 조건을 검사하는 SpEL 표현식
     * true를 반환하면 재시도 실행, false면 재시도 중단
     * 기본값: "true"
     */
    String condition() default "true";

    /**
     * 재시도 실패 시 실행할 fallback 메서드명
     * 같은 클래스 내의 메서드여야 하고, 같은 시그니처를 가져야 함
     * 기본값: ""
     */
    String fallback() default "";

    /**
     * 비동기 재시도 여부
     * true면 별도 스레드에서 재시도 실행
     * 기본값: false
     */
    boolean async() default false;

}