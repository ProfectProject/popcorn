package com.popcorn.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Order 서비스 보안 설정
 *
 * [개발용 설정]
 * - Swagger/Actuator는 공개
 * - 나머지는 인증 필요 (게이트웨이에서 인증/인가 처리)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // REST API이므로 CSRF 비활성화
            .authorizeHttpRequests(authz -> authz
                // Swagger 관련 경로 허용
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Actuator 허용
                .requestMatchers("/actuator/**").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .headers(headers -> headers.frameOptions().sameOrigin());  // H2 Console 허용

        return http.build();
    }
}
