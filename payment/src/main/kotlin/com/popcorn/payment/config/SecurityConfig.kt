package com.popcorn.payment.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.popcorn.common.security.HeaderAuthenticationFilter
import com.popcorn.common.security.JwtAuthenticationFilter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
// CORS 관련 import는 Gateway에서 처리하므로 제거
// import org.springframework.web.cors.CorsConfiguration
// import org.springframework.web.cors.CorsConfigurationSource
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(private val headerAuthenticationFilter: HeaderAuthenticationFilter) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.disable() }
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

                    // Health check 및 Payment API 허용 (JWT 토큰 불필요)
                    .requestMatchers("/api/pay/v*/payments/health").permitAll()
                    .requestMatchers("/api/pay/v1/payments/decode").permitAll()
                    .requestMatchers("/api/pay/v*/**").permitAll()

                    // 나머지는 인증 필요
                    .anyRequest().authenticated()
            }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    // CORS는 Gateway에서 전역적으로 처리
    // @Bean
    // fun corsConfigurationSource(): CorsConfigurationSource { ... }

    @Bean
    fun jwtAuthenticationFilterRegistration(
        filter: JwtAuthenticationFilter
    ): FilterRegistrationBean<JwtAuthenticationFilter> {
        return FilterRegistrationBean(filter).apply {
            isEnabled = false
        }
    }
}
