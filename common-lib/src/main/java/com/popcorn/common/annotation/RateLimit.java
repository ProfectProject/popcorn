package com.popcorn.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 요청 제한을 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 지정된 시간 내 요청 횟수를 제한합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @RateLimit(
 *     requests = 10,
 *     window = 60,
 *     keyExpression = "#request.userId"
 * )
 * public CreateOrderResponse createOrder(CreateOrderRequest request) {
 *     // 주문 생성 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 허용되는 최대 요청 수
     */
    int requests() default 100;

    /**
     * 시간 윈도우 (초)
     */
    int window() default 60;

    /**
     * Rate Limit 키를 생성하기 위한 SpEL 표현식
     * 기본값은 IP 주소 기반
     */
    String keyExpression() default "T(org.springframework.web.context.request.RequestContextHolder).currentRequestAttributes().getRequest().getRemoteAddr()";

    /**
     * Rate Limit 키 접두사
     */
    String keyPrefix() default "rate_limit";

    /**
     * Rate Limit 초과 시 예외 타입
     */
    Class<? extends RuntimeException> exceptionType() default RuntimeException.class;

    /**
     * Rate Limit 초과 시 에러 메시지
     */
    String errorMessage() default "Rate limit exceeded";

    /**
     * Rate Limit 알고리즘
     */
    Algorithm algorithm() default Algorithm.SLIDING_WINDOW;

    /**
     * Rate Limit 알고리즘 열거형
     */
    enum Algorithm {
        /**
         * 고정 윈도우: 정해진 시간 간격으로 카운터 리셋
         */
        FIXED_WINDOW,

        /**
         * 슬라이딩 윈도우: 시간이 지남에 따라 윈도우가 이동
         */
        SLIDING_WINDOW,

        /**
         * 토큰 버킷: 일정한 속도로 토큰 보충, 요청 시 토큰 소모
         */
        TOKEN_BUCKET
    }
}