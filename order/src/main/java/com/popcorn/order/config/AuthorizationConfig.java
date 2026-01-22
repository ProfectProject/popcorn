package com.popcorn.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 권한 검증 관련 설정
 *
 * AOP를 활용한 메소드 레벨 권한 검증을 위한 설정
 */
@Configuration
@EnableAspectJAutoProxy
public class AuthorizationConfig {

    // AOP 자동 프록시 활성화
    // @CheckAuth 애노테이션이 붙은 메소드들에 대해 권한 검증 로직이 실행됩니다.
}