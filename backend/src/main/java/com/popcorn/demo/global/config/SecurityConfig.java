package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * - 개발 환경(local)에서는 보안 비활성화
 * - Swagger UI 및 API 엔드포인트 접근 허용
 * - 프로덕션 환경에서는 별도 보안 설정 적용
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * 개발 환경용 Security 설정 (local 프로파일)
	 * - 모든 요청 허용
	 * - CSRF 비활성화
	 */
	@Bean
	@Profile({"local", "test"})
	public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.anyRequest().permitAll())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.build();
	}

	/**
	 * 개발/테스트 환경용 Security 설정 (dev 프로파일)
	 * - API 엔드포인트는 허용, 관리 기능은 보호
	 */
	@Bean
	@Profile("dev")
	public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**",
								"/swagger-resources/**",
								"/webjars/**"
						).permitAll()
						.requestMatchers("/api/v1/orders/**").permitAll()
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated()
				)
				.httpBasic(basic -> basic.disable())
				.build();
	}

	/**
	 * 프로덕션 환경용 Security 설정 (prod 프로파일)
	 * - 강화된 보안 설정
	 * - JWT 인증 등 적용 예정
	 */
	@Bean
	@Profile("prod")
	public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll()
				)
				.httpBasic(basic -> basic.disable())
				.build();
	}
}
