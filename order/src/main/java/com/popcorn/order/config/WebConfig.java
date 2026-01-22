package com.popcorn.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 관련 설정 (CORS 포함)
 *
 * [초보자 가이드]
 * CORS (Cross-Origin Resource Sharing):
 * - 다른 도메인에서 API에 접근할 때 필요한 보안 설정
 * - 브라우저가 다른 origin의 리소스 요청을 차단하는 것을 허용
 * - 예: Gateway(8080)에서 Order 서비스(8084) 접근
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS 설정
     * Gateway와 Swagger UI에서 Order 서비스 접근 허용
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // Order service API paths
                .allowedOriginPatterns("*")  // 모든 origin 허용 (개발용)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/v3/api-docs/**")  // Swagger API docs
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/swagger-ui/**")  // Swagger UI
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}