package com.popcorn.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security 설정
 * - 개발 환경(local)에서는 보안 비활성화
 * - Swagger UI 및 API 엔드포인트 접근 허용
 * - 프로덕션 환경에서는 별도 보안 설정 적용
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	/**
	 * 개발 환경용 Security 설정 (local 프로파일)
	 * - 모든 요청 허용
	 * - CSRF 비활성화
	 */
	@Bean
	@Profile({"local", "test"})
	public SecurityWebFilterChain localSecurityFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.anyExchange().permitAll())
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.build();
	}

	/**
	 * 개발/테스트 환경용 Security 설정 (dev 프로파일)
	 * - API 엔드포인트는 허용, 관리 기능은 보호
	 */
	@Bean
	@Profile("dev")
	public SecurityWebFilterChain devSecurityFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.pathMatchers(
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**",
								"/swagger-resources/**",
								"/webjars/**"
						).permitAll()
						.pathMatchers("/api/v1/orders/**").permitAll()
						.pathMatchers("/actuator/health").permitAll()
						.anyExchange().authenticated()
				)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.build();
	}

	/**
	 * 프로덕션 환경용 Security 설정 (prod 프로파일)
	 * - 강화된 보안 설정
	 * - JWT 인증 등 적용 예정
	 */
	@Bean
	@Profile("prod")
	public SecurityWebFilterChain prodSecurityFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.pathMatchers("/actuator/health").permitAll()
						.pathMatchers("/api/**").authenticated()
						.anyExchange().denyAll()
				)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.build();
	}
}
