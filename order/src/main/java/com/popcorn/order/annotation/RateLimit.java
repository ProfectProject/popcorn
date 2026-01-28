package com.popcorn.order.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 요청 속도 제한 어노테이션
 *
 * 특정 시간 내에 허용되는 최대 요청 수를 제한합니다.
 * DDoS 공격 방지, 서버 자원 보호, 공정한 서비스 이용을 위해 사용됩니다.
 *
 * 사용 예시:
 * {@literal @}RateLimit(requests = 100, windowSeconds = 3600)  // 시간당 100회
 * public OrderDetailResponse getOrder(UUID orderId) { ... }
 *
 * {@literal @}RateLimit(requests = 5, windowSeconds = 60, keyType = KEY_TYPE.USER_IP)
 * public void createOrder(OrderCreateRequest request) { ... }
 *
 * Rate Limiting 알고리즘:
 * 1. Token Bucket: 일정한 속도로 토큰을 생성하여 버킷에 저장
 * 2. Sliding Window: 시간 창을 슬라이딩하며 요청 수 계산
 * 3. Fixed Window: 고정된 시간 창에서 요청 수 제한
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 시간 창 내에서 허용되는 최대 요청 수
     * 기본값: 100
     */
    int requests() default 100;

    /**
     * 시간 창 크기 (초 단위)
     * 기본값: 3600 (1시간)
     */
    int windowSeconds() default 3600;

    /**
     * Rate Limit 키 생성 방식
     * 기본값: USER_ID (사용자별 제한)
     */
    KeyType keyType() default KeyType.USER_ID;

    /**
     * 커스텀 키 접두사
     * keyType과 함께 사용하여 더 세분화된 제한 가능
     * 기본값: ""
     */
    String keyPrefix() default "";

    /**
     * 제한 초과 시 사용자에게 보여줄 메시지
     * 기본값: ""
     */
    String message() default "";

    /**
     * 제한 적용 여부를 동적으로 결정하는 SpEL 표현식
     * true를 반환하면 제한 적용, false면 제한 무시
     * 기본값: "true"
     */
    String condition() default "true";

    /**
     * Rate Limit 키 생성 방식
     */
    enum KeyType {
        /** 사용자 ID 기반 제한 (로그인 사용자만) */
        USER_ID,

        /** 클라이언트 IP 주소 기반 제한 */
        CLIENT_IP,

        /** 사용자 ID + IP 조합 제한 */
        USER_IP,

        /** 전역 제한 (모든 사용자 공통) */
        GLOBAL,

        /** API 경로별 제한 */
        API_PATH,

        /** 세션 기반 제한 */
        SESSION,

        /** 커스텀 키 (SpEL 표현식 사용) */
        CUSTOM
    }

}
