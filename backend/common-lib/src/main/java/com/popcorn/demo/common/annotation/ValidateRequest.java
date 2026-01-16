package com.popcorn.demo.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청 검증을 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 요청 데이터 검증을 수행합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @ValidateRequest(
 *     groups = {CreateGroup.class},
 *     validateNulls = true,
 *     customValidator = OrderValidator.class
 * )
 * public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
 *     // 주문 생성 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateRequest {

    /**
     * 검증 그룹 클래스들
     */
    Class<?>[] groups() default {};

    /**
     * null 값 검증 여부
     */
    boolean validateNulls() default true;

    /**
     * 빈 값 검증 여부
     */
    boolean validateEmpty() default true;

    /**
     * 커스텀 검증기 클래스
     */
    Class<?>[] customValidator() default {};

    /**
     * 검증 실패 시 예외 타입
     */
    Class<? extends RuntimeException> exceptionType() default IllegalArgumentException.class;

    /**
     * 검증 실패 시 에러 메시지
     */
    String errorMessage() default "Request validation failed";

    /**
     * 특정 파라미터만 검증 (파라미터 이름 지정)
     */
    String[] includeParams() default {};

    /**
     * 검증에서 제외할 파라미터들
     */
    String[] excludeParams() default {};

    /**
     * 검증 순서
     */
    int order() default 0;
}