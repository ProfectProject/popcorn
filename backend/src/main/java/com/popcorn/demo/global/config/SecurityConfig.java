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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.popcorn.demo.domain.auth.jwt.JwtFilter;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import com.popcorn.demo.domain.auth.jwt.LoginFilter;

import lombok.RequiredArgsConstructor;
import java.util.List;
//TODO: 각 domian
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
        loginFilter.setFilterProcessesUrl("/api/v1/auth/login");

		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight 요청 허용
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

						// Popup domain - Authenticated endpoints (all user roles can browse popups)
						.requestMatchers(HttpMethod.GET, "/api/v1/popups").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/popups/{popupId}").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/popups/{popupId}/sessions").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")

						// Swagger UI 관련 엔드포인트 허용
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
						// Actuator 엔드포인트 허용
						.requestMatchers("/actuator/**").permitAll()

						// Order domain - Customer endpoints
						.requestMatchers("/api/v1/orders/me").hasAnyRole("CUSTOMER")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/orders/{orderId}/cancel").hasAnyRole("CUSTOMER")
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/status").hasAnyRole("CUSTOMER")

						// Order domain - Owner/Manager endpoints for store operations
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/status/ops").hasAnyRole("OWNER", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/status/ops").hasAnyRole("OWNER", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/store").hasAnyRole("OWNER", "MANAGER")

						// Order domain - Create orders and payments (all authenticated users can create)
						.requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/pay").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/orders/{orderId}/payments").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}/payments").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/payments/{paymentId}").permitAll()
						.requestMatchers(HttpMethod.PATCH, "/api/v1/payments/{paymentId}/status").permitAll()
						.requestMatchers(HttpMethod.DELETE, "/api/v1/payments/{paymentId}").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/payments/toss/confirm").permitAll()

						// Order domain - Status updates (Owner/Manager can change status)
						.requestMatchers(HttpMethod.PATCH, "/api/v1/orders/{orderId}/status").hasAnyRole("OWNER", "MANAGER")

						// Order domain - Get order details (all authenticated users, but service layer will filter by ownership)
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")

						// Order domain - Development/Testing endpoints
						.requestMatchers(HttpMethod.DELETE, "/api/v1/orders/all").hasAnyRole("OWNER")

						// Order domain - Hidden APIs (Event system) - Owner only
						.requestMatchers("/api/v1/orders/events/**").hasAnyRole("OWNER")

						// Order domain - Hidden APIs (Idempotency) - Owner only
						.requestMatchers("/api/v1/orders/idempotency/**").hasAnyRole("OWNER")

						.anyRequest().authenticated()
				)
				.formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// JWTFilter 추가
		http.addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

		// ★ 로그인 필터 추가
		http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
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
