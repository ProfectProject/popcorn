package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;

import java.util.List;
import java.util.ArrayList;

@Configuration
@OpenAPIDefinition(
		info = @io.swagger.v3.oas.annotations.info.Info(
				title = "POPCORN API",
				version = "v1",
				description = "POPCORN Backend API with JWT Authentication"
		),
		tags = {
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth", description = "인증 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "User", description = "사용자 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Order", description = "주문 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "QR", description = "QR 코드 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Checkin", description = "체크인 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Popup", description = "팝업 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Inventory", description = "재고 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Stores", description = "매장 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "OwnerPopupController", description = "사장님 팝업 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Goods", description = "굿즈 관리 API"),
				@io.swagger.v3.oas.annotations.tags.Tag(name = "Payments", description = "결제 관리 API")
		}
)
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
				.addSecurityItem(securityRequirement)
				.components(new io.swagger.v3.oas.models.Components()
						.addSecuritySchemes("Bearer Authentication", jwtSecurityScheme));
	}

	@Bean
	public OpenApiCustomizer customizeOpenApi() {
		return openApi -> {
			List<Tag> orderedTags = new ArrayList<>();

			// 원하는 순서대로 태그 정의
			orderedTags.add(new Tag().name("Auth").description("인증 관리 API"));
			orderedTags.add(new Tag().name("User").description("사용자 관리 API"));
			orderedTags.add(new Tag().name("Order").description("주문 관리 API"));
			orderedTags.add(new Tag().name("Payments").description("결제 관리 API"));
			orderedTags.add(new Tag().name("QR").description("QR 코드 관리 API"));
			orderedTags.add(new Tag().name("Checkin").description("체크인 관리 API"));
			orderedTags.add(new Tag().name("Popup").description("팝업 관리 API"));
			orderedTags.add(new Tag().name("Inventory").description("재고 관리 API"));
			orderedTags.add(new Tag().name("Stores").description("매장 관리 API"));
			orderedTags.add(new Tag().name("OwnerPopupController").description("사장님 팝업 관리 API"));
			orderedTags.add(new Tag().name("Goods").description("굿즈 관리 API"));

			openApi.setTags(orderedTags);
		};
	}


}

