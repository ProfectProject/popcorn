package com.popcorn.demo.presentation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.popcorn.demo.common.config.OpenApiConfig;
import com.popcorn.demo.presentation.interceptor.RequestLoggingInterceptor;

@Configuration

@EnableWebFlux

@Import(OpenApiConfig.class)

@ComponentScan(basePackages = "com.popcorn.demo.presentation")

public class PresentationConfig implements WebFluxConfigurer {



	@Bean

	public RequestLoggingInterceptor requestLoggingInterceptor() {

		return new RequestLoggingInterceptor();

	}



	@Bean

	public ObjectMapper objectMapper() {

		ObjectMapper mapper = new ObjectMapper();

		mapper.findAndRegisterModules();

		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		return mapper;

	}



	@Override

	public void addCorsMappings(CorsRegistry registry) {

		registry.addMapping("/api/**")

				.allowedOriginPatterns("*")

				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

				.allowedHeaders("*")

				.exposedHeaders("Idempotency-Key")

				.allowCredentials(false)

				.maxAge(3600);

	}

}
