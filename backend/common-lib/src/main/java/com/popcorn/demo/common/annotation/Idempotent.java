package com.popcorn.demo.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 멱등성 처리를 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 동일한 요청에 대해 멱등성을 보장합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @Idempotent(keyExpression = "#request.orderId")
 * public CreateOrderResponse createOrder(CreateOrderRequest request) {
 *     // 주문 생성 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 멱등성 키를 생성하기 위한 SpEL 표현식
     *
     * @return SpEL 표현식 (예: "#request.orderId", "#p0.id")
     */
    String keyExpression() default "";

    /**
     * 정적 멱등성 키 (keyExpression보다 우선순위 낮음)
     *
     * @return 고정 키 값
     */
    String staticKey() default "";

    /**
     * 캐시 만료 시간 (초)
     *
     * @return 만료 시간 (기본값: 1800초 = 30분)
     */
    int ttlSeconds() default 1800;

    /**
     * 멱등성 키 접두사
     *
     * @return 접두사 (기본값: "idempotent")
     */
    String keyPrefix() default "idempotent";

    /**
     * 실패 시 재시도 허용 여부
     * true인 경우, 이전 요청이 실패했다면 재시도를 허용합니다.
     *
     * @return 재시도 허용 여부 (기본값: true)
     */
    boolean retryOnFailure() default true;

    /**
     * 응답 타입 클래스 (직렬화/역직렬화용)
     *
     * @return 응답 타입 (기본값: Object.class)
     */
    Class<?> responseType() default Object.class;
}