package com.popcorn.demo.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 로깅을 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 적용하면 요청/응답 로깅을 수행합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @ApiLogging(level = LogLevel.INFO, includeRequest = true, includeResponse = true)
 * public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
 *     // 주문 조회 로직
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLogging {

    /**
     * 로그 레벨
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 요청 파라미터 로깅 여부
     */
    boolean includeRequest() default true;

    /**
     * 응답 데이터 로깅 여부
     */
    boolean includeResponse() default true;

    /**
     * 실행 시간 로깅 여부
     */
    boolean includeExecutionTime() default true;

    /**
     * 예외 발생 시 로깅 여부
     */
    boolean includeException() default true;

    /**
     * 로깅에서 제외할 파라미터 이름들
     */
    String[] excludeParams() default {};

    /**
     * 민감 정보 마스킹 여부
     */
    boolean maskSensitiveData() default true;

    /**
     * 커스텀 로그 메시지
     */
    String message() default "";

    /**
     * 로그 레벨 열거형
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}