package com.popcorn.demo.common.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 버전 지정 어노테이션
 *
 * 컨트롤러 메서드나 클래스에 적용하여 API 버전을 명시
 *
 * 사용 예:
 * - @ApiVersion("v1") - v1 버전으로 지정
 * - @ApiVersion({"v1", "v2"}) - v1, v2 모두 지원
 * - @ApiVersion(value = "v2", deprecatedInVersions = "v3") - v2에서 지원, v3에서 deprecated
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    /**
     * 지원하는 API 버전들
     */
    String[] value() default {"v1"};

    /**
     * deprecated로 표시될 버전 (해당 버전부터 deprecated)
     */
    String deprecatedInVersions() default "";

    /**
     * 제거 예정 버전 (해당 버전부터 제거)
     */
    String removedInVersions() default "";

    /**
     * 버전별 추가 설명
     */
    String description() default "";
}