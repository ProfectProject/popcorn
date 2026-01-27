package com.popcorn.order.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 성능 모니터링 어노테이션
 *
 * 메서드의 실행 시간을 측정하고 로그에 기록합니다.
 * 느린 쿼리나 성능 병목 지점을 찾는 데 도움이 됩니다.
 *
 * 사용 예시:
 * {@literal @}PerformanceMonitoring
 * public OrderDetailResponse getOrderDetail(UUID orderId) { ... }
 *
 * {@literal @}PerformanceMonitoring(threshold = 1000, logParams = true)
 * public void processLargeOrder(OrderCreateRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceMonitoring {

    /**
     * 경고 임계값 (밀리초)
     * 이 시간을 초과하면 WARNING 레벨로 로그가 기록됩니다.
     * 기본값: 500ms
     */
    long threshold() default 500;

    /**
     * 메서드 파라미터를 로그에 포함할지 여부
     * 민감한 정보가 있는 경우 false로 설정하세요.
     * 기본값: false
     */
    boolean logParams() default false;

    /**
     * 메서드 반환값을 로그에 포함할지 여부
     * 큰 객체인 경우 false로 설정하는 것이 좋습니다.
     * 기본값: false
     */
    boolean logResult() default false;

    /**
     * 성능 메트릭을 수집할 카테고리
     * 메트릭 대시보드에서 그룹화할 때 사용됩니다.
     * 기본값: "general"
     */
    String category() default "general";

    /**
     * 커스텀 메트릭 이름
     * 지정하지 않으면 클래스명.메서드명이 사용됩니다.
     * 기본값: ""
     */
    String metricName() default "";

}
