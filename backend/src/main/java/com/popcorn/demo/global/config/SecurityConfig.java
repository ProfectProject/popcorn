package com.popcorn.demo.global.config;

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

import com.popcorn.demo.domain.auth.jwt.JwtFilter;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import com.popcorn.demo.domain.auth.jwt.LoginFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthenticationConfiguration authenticationConfiguration;
	private final JwtUtil jwtUtil;

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
        loginFilter.setFilterProcessesUrl("/api/auth/login");

		http.csrf().disable()
				.authorizeHttpRequests()
				.requestMatchers("/api/auth/login").permitAll()
				.requestMatchers("/api/v1/auth/login").permitAll() // swagger api 테스트
				.requestMatchers("/api/v1/users/signup").permitAll()
				.requestMatchers("/api/v1/users/**").hasAnyRole("CUSTOMER", "OWNER")

				// QR
				.requestMatchers(HttpMethod.POST, "/api/v1/qr/verify").permitAll()
				.requestMatchers(HttpMethod.GET,  "/api/v1/orders/*/qr").hasRole("CUSTOMER")
				.requestMatchers(HttpMethod.POST, "/api/v1/orders/*/qr").permitAll()

				// Checkin
				.requestMatchers(HttpMethod.GET, "/api/v1/checkins").hasAnyRole("OWNER", "ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/v1/checkins/{checkinId}").permitAll()

				// Swagger UI 관련 엔드포인트 허용
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
				// Actuator 엔드포인트 허용
				.requestMatchers("/actuator/**").permitAll()

				// Order domain - Customer endpoints
				.requestMatchers("/api/v1/orders/me").hasAnyRole("CUSTOMER")
				.requestMatchers(HttpMethod.DELETE, "/api/v1/orders/{orderId}/cancel").hasAnyRole("CUSTOMER")
				.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/status").hasAnyRole("CUSTOMER")

				// Order domain - Owner/Manager/Admin endpoints for store operations
				.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/status/ops").hasAnyRole("OWNER", "MANAGER", "ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/v1/orders/status/ops").hasAnyRole("OWNER", "MANAGER", "ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/v1/orders/store").hasAnyRole("OWNER", "MANAGER", "ADMIN")

				// Order domain - Create orders and payments (all authenticated users can create)
				.requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("CUSTOMER", "OWNER", "MANAGER", "ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/v1/orders/{orderId}/reservation-payments").hasAnyRole("CUSTOMER")
				.requestMatchers(HttpMethod.POST, "/api/v1/orders/{orderId}/payments").hasAnyRole("CUSTOMER")
				.requestMatchers(HttpMethod.POST, "/api/v1/orders/{orderId}/payments/ready").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/payments").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/payments/{paymentId}").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/payments/{paymentId}/approve").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/payments/{paymentId}/fail").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/payments/{paymentId}/cancel").permitAll()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/payments/{paymentId}").permitAll()

				// Order domain - Status updates (Owner/Manager/Admin can change status)
				.requestMatchers(HttpMethod.PATCH, "/api/v1/orders/{orderId}/status").hasAnyRole("OWNER", "MANAGER", "ADMIN")

				// Order domain - Get order details (all authenticated users, but service layer will filter by ownership)
				.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}").hasAnyRole("CUSTOMER", "OWNER", "MANAGER", "ADMIN")

				// Order domain - Development/Testing endpoints
				.requestMatchers(HttpMethod.DELETE, "/api/v1/orders/all").hasAnyRole("ADMIN")

				// Order domain - Hidden APIs (Event system) - Admin only
				.requestMatchers("/api/v1/orders/events/**").hasAnyRole("ADMIN")

				// Order domain - Hidden APIs (Idempotency) - Admin only
				.requestMatchers("/api/v1/orders/idempotency/**").hasAnyRole("ADMIN")

				.anyRequest().authenticated()
				.and()
				.formLogin().disable()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		// JWTFilter 추가
		http.addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

		// ★ 로그인 필터 추가
		http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
