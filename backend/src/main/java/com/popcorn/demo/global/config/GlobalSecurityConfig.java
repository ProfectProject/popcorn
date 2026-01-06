/*package com.popcorn.demo.global.config;

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

@Configuration
@EnableWebSecurity
public class GlobalSecurityConfig {

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
}
*/