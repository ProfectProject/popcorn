package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration

public class OpenApiConfig {



	@Bean
	public OpenAPI popcornOpenApi() {
		// JWT Security Scheme 정의
		SecurityScheme jwtSecurityScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.name("Authorization")
				.description("JWT Token Authentication");

		// Security Requirement 정의
		SecurityRequirement securityRequirement = new SecurityRequirement()
				.addList("Bearer Authentication");

		return new OpenAPI()
				.info(new Info()
						.title("POPCORN API")
						.version("v1")
						.description("POPCORN Backend API with JWT Authentication"))
				.addSecurityItem(securityRequirement)
				.components(new io.swagger.v3.oas.models.Components()
						.addSecuritySchemes("Bearer Authentication", jwtSecurityScheme));
	}

}

