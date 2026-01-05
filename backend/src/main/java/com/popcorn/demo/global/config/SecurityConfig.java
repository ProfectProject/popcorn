package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.anyRequest().permitAll())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
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
				.csrf(AbstractHttpConfigurer::disable)
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
				.httpBasic(AbstractHttpConfigurer::disable)
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
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll()
				)
				.httpBasic(AbstractHttpConfigurer::disable)
				.build();
	}

	/**
	 * 개발/테스트 환경용 더미 UserDetailsService
	 * UserDetailsServiceAutoConfiguration 경고 해결
	 */
	@Bean
	@Profile({"local", "test"})
	public UserDetailsService localUserDetailsService() {
		// 개발 환경에서는 실제 인증을 사용하지 않으므로 더미 서비스 제공
		return username -> {
			// 개발 환경에서는 모든 요청이 permitAll()이므로 실제로 호출되지 않음
			throw new UsernameNotFoundException("Development mode - authentication disabled");
		};
	}

	/**
	 * 개발 환경용 UserDetailsService (dev 프로파일)
	 * 기본적인 인메모리 사용자 제공
	 */
	@Bean
	@Profile("dev")
	public UserDetailsService devUserDetailsService(PasswordEncoder passwordEncoder) {
		UserDetails user = User.builder()
				.username("admin")
				.password(passwordEncoder.encode("admin123"))
				.roles("ADMIN")
				.build();

		UserDetails storeOwner = User.builder()
				.username("owner")
				.password(passwordEncoder.encode("owner123"))
				.roles("STORE_OWNER")
				.build();

		return new InMemoryUserDetailsManager(user, storeOwner);
	}

	/**
	 * 프로덕션 환경용 UserDetailsService (prod 프로파일)
	 * 실제 사용자 데이터베이스와 연동 예정
	 */
	@Bean
	@Profile("prod")
	public UserDetailsService prodUserDetailsService() {
		// TODO: 실제 사용자 서비스와 연동
		return username -> {
			throw new UsernameNotFoundException("User service not implemented yet: " + username);
		};
	}

	/**
	 * 비밀번호 인코더
	 */
	@Bean
	@Profile({"dev", "prod"})
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
