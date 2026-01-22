package com.popcorn.checkIns.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Swagger 관련 경로 허용
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // 액추에이터 허용
                .requestMatchers("/actuator/**").permitAll()
                // QR 관련 API 허용 (현재는 모든 QR API를 허용, 추후 인증 추가 가능)
                .requestMatchers("/api/v*/qr/**", "/api/v*/orders/*/qr", "/api/v*/orders/*/qr/**", "/api/v*/checkin/**", "/api/v*/checkins/**").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
}