package com.popcorn.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

		* - Swagger UI 접근 허용

		*/

	@Bean

	@Profile("local")

	public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {

		http

			// CSRF 보호 비활성화 (개발 환경)

			.csrf(AbstractHttpConfigurer::disable)



			// 모든 HTTP 요청 허용

			.authorizeHttpRequests(authz -> authz

				.requestMatchers("/**").permitAll()  // 모든 경로 허용

				.anyRequest().permitAll()

			)



			// HTTP Basic 인증 비활성화

			.httpBasic(AbstractHttpConfigurer::disable)



			// Form 로그인 비활성화

			.formLogin(AbstractHttpConfigurer::disable);



		return http.build();

	}



	/**

		* 개발/테스트 환경용 Security 설정 (dev 프로파일)

		* - API 엔드포인트는 허용, 관리 기능은 보호

		*/

	@Bean

	@Profile("dev")

	public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {

		http

			.csrf(AbstractHttpConfigurer::disable)

			.authorizeHttpRequests(authz -> authz

				// Swagger UI 및 OpenAPI 문서 허용

				.requestMatchers(

					"/swagger-ui/**",

					"/swagger-ui.html",

					"/v3/api-docs/**",

					"/swagger-resources/**",

					"/webjars/**"

				).permitAll()



				// 주문 API 허용

				.requestMatchers("/api/v1/orders/**").permitAll()



				// Actuator health check 허용

				.requestMatchers("/actuator/health").permitAll()



				// 기타 요청은 인증 필요

				.anyRequest().authenticated()

			)

			.httpBasic(httpBasic -> { }); // HTTP Basic 인증 활성화



		return http.build();

	}



	/**

		* 프로덕션 환경용 Security 설정 (prod 프로파일)

		* - 강화된 보안 설정

		* - JWT 인증 등 적용 예정

		*/

	@Bean

	@Profile("prod")

	public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {

		http

			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // API는 CSRF 예외

			.authorizeHttpRequests(authz -> authz

				// Health check만 허용

				.requestMatchers("/actuator/health").permitAll()



				// 모든 API 요청은 인증 필요

				.requestMatchers("/api/**").authenticated()



				// 기타 모든 요청 거부

				.anyRequest().denyAll()

			)

			.httpBasic(httpBasic -> { }); // 프로덕션에서는 JWT 등으로 교체 예정



		return http.build();

	}

}

