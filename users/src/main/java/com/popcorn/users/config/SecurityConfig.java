package com.popcorn.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.popcorn.common.filter.HeaderAuthenticationFilter;

//import com.popcorn.users.auth.jwt.JwtFilter;
import com.popcorn.users.auth.jwt.JwtUtil;
import com.popcorn.users.auth.jwt.LoginFilter;
import com.popcorn.common.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import java.util.List;
//TODO: 각 domian
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthenticationConfiguration authenticationConfiguration;
	private final JwtUtil jwtUtil;
	private final HeaderAuthenticationFilter headerAuthenticationFilter;

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		AuthenticationManager authManager = authenticationManager(authenticationConfiguration);

		// ★ LoginFilter는 여기서 직접 생성 (Bean 등록 X)
        LoginFilter loginFilter = new LoginFilter(authManager, jwtUtil);
        loginFilter.setFilterProcessesUrl("/api/v1/auth/login");

		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight 요청 허용
						.requestMatchers("/api/auth/login").permitAll()
						.requestMatchers("/api/users/v1/auth/login").permitAll() // swagger api 테스트
						.requestMatchers("/api/users/v1/users/signup").permitAll()
						.requestMatchers("/api/users/v1/users/**").permitAll()
						//.requestMatchers("/api/users/v1/users/**").hasAnyRole("CUSTOMER", "OWNER")


						// Swagger UI 관련 엔드포인트 허용 (Gateway 재작성 경로 포함)
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs", "/swagger-resources/**", "/webjars/**").permitAll()
						.requestMatchers("/api/users/v3/api-docs/**", "/api/users/v3/api-docs", "/api/users/swagger-ui/**", "/api/users/swagger-ui.html").permitAll()
						// Actuator 엔드포인트 허용
						.requestMatchers("/actuator/**").permitAll()
						.anyRequest().authenticated()
				)
				.formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// Gateway 인증 헤더 기반 필터
		http.addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		// ★ 로그인 필터 추가
		http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
			JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("*"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
