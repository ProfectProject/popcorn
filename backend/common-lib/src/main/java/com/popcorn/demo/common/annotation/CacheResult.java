package com.popcorn.demo.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 결과 캐싱을 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 실행 결과를 캐싱합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @CacheResult(
 *     cacheName = "orderCache",
 *     keyExpression = "#orderId",
 *     ttlSeconds = 300
 * )
 * public OrderResponse getOrder(String orderId) {
 *     // 주문 조회 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheResult {

    /**
     * 캐시 이름
     */
    String cacheName();

    /**
     * 캐시 키를 생성하기 위한 SpEL 표현식
     */
    String keyExpression() default "";

    /**
     * 정적 캐시 키 (keyExpression보다 우선순위 낮음)
     */
    String staticKey() default "";

    /**
     * 캐시 만료 시간 (초)
     * -1인 경우 만료시간 없음
     */
    int ttlSeconds() default 600;

    /**
     * null 값 캐싱 여부
     */
    boolean cacheNull() default false;

    /**
     * 캐시 조건 SpEL 표현식
     * 이 조건이 true인 경우에만 캐싱
     */
    String condition() default "";

    /**
     * 캐시 제외 조건 SpEL 표현식
     * 이 조건이 true인 경우 캐싱하지 않음
     */
    String unless() default "";

    /**
     * 캐시 키 접두사
     */
    String keyPrefix() default "";

    /**
     * 캐시 전략
     */
    Strategy strategy() default Strategy.READ_THROUGH;

    /**
     * 캐시 전략 열거형
     */
    enum Strategy {
        /**
         * Read-Through: 캐시 미스 시 원본 메서드 호출 후 캐싱
         */
        READ_THROUGH,

        /**
         * Write-Around: 캐시를 우회하여 직접 저장소에 쓰기
         */
        WRITE_AROUND,

        /**
         * Write-Through: 캐시와 저장소에 동시 쓰기
         */
        WRITE_THROUGH,

        /**
         * Write-Behind: 캐시에 먼저 쓰고 나중에 저장소에 쓰기
         */
        WRITE_BEHIND
    }
}