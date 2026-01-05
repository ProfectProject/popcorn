package com.popcorn.demo.domain.order.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.RequiredArgsConstructor;

/**
 * 주문 도메인 설정 클래스
 *
 * 매직 넘버 제거 및 환경별 설정 관리:
 * - 코드 기반 설정으로 타입 안전성 확보
 * - 환경별 프로파일 적용
 * - IDE 자동완성 및 검증 지원
 */
@Configuration
@EnableConfigurationProperties(OrderProperties.class)
@RequiredArgsConstructor
public class OrderConfiguration {

	/**
	 * Local 환경 전용 주문 설정 (개발용 최적화)
	 */
	@Bean
	@Profile("local")
	public OrderProperties localOrderProperties() {
		OrderProperties properties = new OrderProperties();

		// 로컬 환경: 100% 결제 성공률
		properties.setPaymentSuccessRate(1.0);
		properties.setCancelableTimeout(Duration.ofMinutes(15));

		// 개발용 관대한 검증 설정
		OrderProperties.Validation validation = new OrderProperties.Validation();
		validation.setMaxStockQuantity(1000);  // 더 많은 재고 허용
		validation.setMinOrderAmount(100);
		validation.setMaxOrderAmount(1000000);
		properties.setValidation(validation);

		// 로컬 캐싱 설정 (짧은 TTL)
		OrderProperties.Cache cache = new OrderProperties.Cache();
		cache.setOrderDetailTtl(Duration.ofMinutes(1));
		cache.setOrderListTtl(Duration.ofMinutes(1));
		cache.setCustomerTimelineTtl(Duration.ofMinutes(1));
		properties.setCache(cache);

		// 비동기 설정
		OrderProperties.Async async = new OrderProperties.Async();
		async.setTaskTimeout(Duration.ofSeconds(5));
		async.setEventRetryCount(2);
		async.setParallelValidation(true);
		properties.setAsync(async);

		return properties;
	}

	/**
	 * Development 환경 전용 주문 설정
	 */
	@Bean
	@Profile("dev")
	public OrderProperties devOrderProperties() {
		OrderProperties properties = new OrderProperties();

		// 개발 환경: 90% 결제 성공률
		properties.setPaymentSuccessRate(0.9);
		properties.setCancelableTimeout(Duration.ofMinutes(15));

		// 표준 검증 설정
		properties.setValidation(new OrderProperties.Validation());

		// 표준 캐싱 설정
		properties.setCache(new OrderProperties.Cache());

		// 표준 비동기 설정
		properties.setAsync(new OrderProperties.Async());

		return properties;
	}

	/**
	 * Production 환경 전용 주문 설정 (최적화 및 안정성 강화)
	 */
	@Bean
	@Profile("prod")
	public OrderProperties prodOrderProperties() {
		OrderProperties properties = new OrderProperties();

		// 프로덕션: 95% 결제 성공률
		properties.setPaymentSuccessRate(0.95);
		properties.setCancelableTimeout(Duration.ofMinutes(15));

		// 프로덕션 검증 설정 (더 엄격함)
		OrderProperties.Validation validation = new OrderProperties.Validation();
		validation.setMaxStockQuantity(50);   // 더 보수적인 재고 제한
		validation.setMinOrderAmount(100);
		validation.setMaxOrderAmount(500000); // 더 낮은 최대 금액
		properties.setValidation(validation);

		// 프로덕션 캐싱 설정 (긴 TTL)
		OrderProperties.Cache cache = new OrderProperties.Cache();
		cache.setOrderDetailTtl(Duration.ofMinutes(10));
		cache.setOrderListTtl(Duration.ofMinutes(5));
		cache.setCustomerTimelineTtl(Duration.ofMinutes(7));
		properties.setCache(cache);

		// 프로덕션 비동기 설정 (안정성 강화)
		OrderProperties.Async async = new OrderProperties.Async();
		async.setTaskTimeout(Duration.ofSeconds(15));
		async.setEventRetryCount(5);  // 더 많은 재시도
		async.setParallelValidation(true);
		properties.setAsync(async);

		return properties;
	}
}