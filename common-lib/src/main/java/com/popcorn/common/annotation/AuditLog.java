package com.popcorn.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 로그를 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 감사 로그를 기록합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @AuditLog(
 *     action = "ORDER_CREATE",
 *     resource = "Order",
 *     resourceIdExpression = "#result.orderId"
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
public @interface AuditLog {

    /**
     * 수행된 액션
     */
    String action();

    /**
     * 대상 리소스 타입
     */
    String resource();

    /**
     * 리소스 ID를 얻기 위한 SpEL 표현식
     */
    String resourceIdExpression() default "";

    /**
     * 정적 리소스 ID
     */
    String staticResourceId() default "";

    /**
     * 액션 설명
     */
    String description() default "";

    /**
     * 민감 정보 로깅 여부
     */
    boolean includeSensitiveData() default false;

    /**
     * 요청 데이터 포함 여부
     */
    boolean includeRequestData() default true;

    /**
     * 응답 데이터 포함 여부
     */
    boolean includeResponseData() default false;

    /**
     * 사용자 ID를 얻기 위한 SpEL 표현식
     */
    String userIdExpression() default "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()";

    /**
     * 로깅에서 제외할 파라미터들
     */
    String[] excludeParams() default {"password", "token", "secret"};

    /**
     * 감사 로그 레벨
     */
    Level level() default Level.INFO;

    /**
     * 성공 시에만 로깅할지 여부
     */
    boolean onSuccessOnly() default false;

    /**
     * 감사 로그 레벨 열거형
     */
    enum Level {
        DEBUG, INFO, WARN, ERROR
    }
}