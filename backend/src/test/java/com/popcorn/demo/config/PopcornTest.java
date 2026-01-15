package com.popcorn.demo.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * PopCorn 프로젝트 공통 테스트 어노테이션
 *
 * 모든 Spring Boot 통합 테스트가 일관되게 다음 설정을 사용합니다:
 * - TestRedisConfig: Redis Mock 구현체
 * - TestSecurityConfig: 테스트용 Security 설정
 * - application-test.yml: 테스트 환경 설정
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@Import({TestSecurityConfig.class, TestRedisConfig.class})
@TestPropertySource(locations = "classpath:application-test.yml")
public @interface PopcornTest {

    /**
     * SpringBootTest의 classes 속성을 전달
     */
    Class<?>[] classes() default {};

    /**
     * SpringBootTest의 webEnvironment 속성을 전달
     */
    SpringBootTest.WebEnvironment webEnvironment() default SpringBootTest.WebEnvironment.MOCK;

    /**
     * SpringBootTest의 properties 속성을 전달
     */
    String[] properties() default {};
}