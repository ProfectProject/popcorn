package com.popcorn.payment.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { authz ->
                authz
                    // CORS preflight 요청 허용
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Swagger 관련 경로 허용
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll()

                    // 액추에이터 허용
                    .requestMatchers("/actuator/**").permitAll()

                    // Payment 관련 API 모두 허용
                    .requestMatchers("/api/payments/**").permitAll()
                    .requestMatchers("/api/payment/**").permitAll()
                    .requestMatchers("/api/v*/payments/**").permitAll()
                    .requestMatchers("/api/v*/payment/**").permitAll()
                    .requestMatchers("/api/pay/v*/**").permitAll()

                    // Health check 허용
                    .requestMatchers("/api/v*/payments/health").permitAll()

                    // 나머지는 모든 요청 허용 (개발용)
                    .anyRequest().permitAll()
            }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = false
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}