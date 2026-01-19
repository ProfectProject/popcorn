package com.popcorn.demo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;
import java.util.ArrayList;

//TODO: 각 도메인

@Configuration
public class OpenApiConfig {

	/**
	 * 🎭 고객용 API 문서
	 * 접근 URL: /swagger-ui/customer.html
	 * API 문서: /v3/api-docs/customer
	 */
	@Bean
	public GroupedOpenApi customerApi() {
		return GroupedOpenApi.builder()
				.group("customer")
				.displayName("🛒 고객용 API")
				.pathsToMatch(
						// 인증 관련
						"/api/auth/login",
						"/api/v1/auth/login",

						// 사용자 관련 (고객용 모든 API)
						"/api/v1/users/signup",
						"/api/v1/users/mypage",
						"/api/v1/users/me/address",
						"/api/v1/users/me/addresses",
						"/api/v1/users/me/addresses/**",
						"/api/v1/users/me/deactivate",

						// 팝업 조회 (고객이 볼 수 있는)
						"/api/v1/popups",
						"/api/v1/popups/**",

						// 내 주문 관련 (고객 본인만)
						"/api/v1/orders/me",
						"/api/v1/orders/{orderId}",
						"/api/v1/orders/{orderId}/status",
						"/api/v1/orders/{orderId}/cancel",
						"/api/v1/orders/{orderId}/qr",

						// 주문 생성
						"/api/v1/orders",

						// 결제 관련 (고객)
						"/api/v1/orders/{orderId}/pay",
						"/api/v1/orders/{orderId}/payments",
						"/api/v1/payments/**",

						// QR 검증 (고객)
						"/api/v1/qr/verify"
				)
				.addOpenApiCustomizer(openApi -> {
					List<Tag> customerTags = new ArrayList<>();
					customerTags.add(new Tag().name("Auth").description("🔐 고객 인증"));
					customerTags.add(new Tag().name("User").description("👤 회원가입 및 프로필"));
					customerTags.add(new Tag().name("Popup").description("🎪 팝업 조회"));
					customerTags.add(new Tag().name("Goods").description("📦 굿즈 조회"));
					customerTags.add(new Tag().name("Order").description("📦 내 주문 관리"));
					customerTags.add(new Tag().name("Payments").description("💳 결제 관리"));
					customerTags.add(new Tag().name("QR").description("📱 QR 코드"));
					openApi.setTags(customerTags);
				})
				.build();
	}

	/**
	 * 🏪 운영자용 API 문서
	 * 접근 URL: /swagger-ui/manager.html
	 * API 문서: /v3/api-docs/manager
	 */
	@Bean
	public GroupedOpenApi managerApi() {
		return GroupedOpenApi.builder()
				.group("manager")
				.displayName("🏪 운영자용 API")
				.pathsToMatch(
						// 인증 관련
						"/api/auth/login",
						"/api/v1/auth/login",

						// 사용자 관련 (운영자용 - 회원가입만)
						"/api/v1/users/signup",

						// 팝업 조회 (매니저/오너도 조회 가능)
						"/api/v1/popups",
						"/api/v1/popups/**",

						// 매장 주문 관리 (운영자)
						"/api/v1/orders/store",
						"/api/v1/orders/status/ops",
						"/api/v1/orders/{orderId}/status/ops",

						// 팝업별 주문 관리
						"/api/v1/orders/popup/**",

						// 체크인 관리
						"/api/v1/checkins",
						"/api/v1/checkins/**",

						// 매장 관리 (오너 스토어 관리)
						"/api/v1/owner/stores",
						"/api/v1/owner/stores/**",

						// 굿즈 관리
						"/api/v1/goods",
						"/api/v1/goods/**",

						// 재고 관리
						"/api/v1/inventory",
						"/api/v1/inventory/**",

						// 매니저 전용 API (관리자 승인, 강제 중지 등)
						"/api/manager/**",

						// 오너 관련 전체 API
						"/api/v1/owner/**"
				)
				.addOpenApiCustomizer(openApi -> {
					List<Tag> managerTags = new ArrayList<>();
					managerTags.add(new Tag().name("Auth").description("🔐 운영자 인증"));
					managerTags.add(new Tag().name("User").description("👤 사용자 관리"));
					managerTags.add(new Tag().name("유저매니저").description("👤 유저 매니저"));
					managerTags.add(new Tag().name("Popup").description("🎪 팝업 조회 및 관리"));
					managerTags.add(new Tag().name("Order").description("📊 주문 관리"));
					managerTags.add(new Tag().name("Stores").description("🏪 매장 관리"));
					managerTags.add(new Tag().name("Goods").description("📦 굿즈 관리"));
					managerTags.add(new Tag().name("Inventory").description("📋 재고 관리"));
					managerTags.add(new Tag().name("Checkin").description("✅ 체크인 관리"));
					managerTags.add(new Tag().name("매니저팝업").description("🎪 팝업 관리"));
					openApi.setTags(managerTags);
				})
				.build();
	}

	/**
	 * 🔧 전체 API 문서 (개발자용)
	 * 접근 URL: /swagger-ui/admin.html
	 * API 문서: /v3/api-docs/admin
	 */
	@Bean
	public GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()
				.group("admin")
				.displayName("🔧 전체 API (개발자용)")
				.pathsToMatch("/api/**")
				.pathsToExclude("/api/v1/chaos/**", "/api/v1/extreme/**")
				.addOpenApiCustomizer(openApi -> {
					List<Tag> adminTags = new ArrayList<>();
					adminTags.add(new Tag().name("Auth").description("🔐 인증 관리"));
					adminTags.add(new Tag().name("User").description("👤 사용자 관리"));
					adminTags.add(new Tag().name("유저매니저").description("👤 유저 매니저"));
					adminTags.add(new Tag().name("Popup").description("🎪 팝업 관리"));
					adminTags.add(new Tag().name("Order").description("📦 주문 관리"));
					adminTags.add(new Tag().name("Payments").description("💳 결제 관리"));
					adminTags.add(new Tag().name("QR").description("📱 QR 코드"));
					adminTags.add(new Tag().name("Stores").description("🏪 매장 관리"));
					adminTags.add(new Tag().name("Goods").description("📦 굿즈 관리"));
					adminTags.add(new Tag().name("Inventory").description("📋 재고 관리"));
					adminTags.add(new Tag().name("Checkin").description("✅ 체크인 관리"));
					adminTags.add(new Tag().name("OwnerCheckin").description("🔍 오너 체크인 조회"));
					adminTags.add(new Tag().name("매니저팝업").description("🎪 매니저 팝업 관리"));
					openApi.setTags(adminTags);
				})
				.build();
	}

	@Bean
	public OpenAPI popcornOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("🍿 POPCORN API")
						.version("v1.0.0")
						.description("""
								POPCORN 팝업 스토어 플랫폼 API

								## 🎯 역할별 API 문서
								- **고객용**: 팝업 조회, 주문, 결제 등 고객 기능
								- **운영자용**: 매장 관리, 주문 관리, 재고 관리 등 운영 기능
								- **전체**: 모든 API (개발자용)

								## 🔐 인증 방식
								Bearer Token (JWT) 사용
								"""))
				.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
				.components(new io.swagger.v3.oas.models.Components()
						.addSecuritySchemes("Bearer Authentication",
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.name("Authorization")
										.description("JWT Token Authentication")));
	}

}
