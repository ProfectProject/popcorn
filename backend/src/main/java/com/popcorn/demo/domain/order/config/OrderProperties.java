package com.popcorn.demo.domain.order.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 주문 관련 설정 프로퍼티
 *
 * 매직 넘버 제거를 위한 설정값 외부화:
 * - 취소 가능 시간, 페이지 크기 등 설정
 * - 환경별 다른 값 적용 가능
 * - 런타임 설정 변경 지원
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.order")
public class OrderProperties {

	/**
	 * 주문 취소 가능 시간 (기본: 15분)
	 */
	private Duration cancelableTimeout = Duration.ofMinutes(15);

	/**
	 * 결제 성공률 (개발/테스트용, 기본: 90%)
	 */
	private double paymentSuccessRate = 0.9;

	/**
	 * 페이징 설정
	 */
	private Pagination pagination = new Pagination();

	/**
	 * 검증 설정
	 */
	private Validation validation = new Validation();

	/**
	 * 캐싱 설정
	 */
	private Cache cache = new Cache();

	/**
	 * 비동기 처리 설정
	 */
	private Async async = new Async();

	@Data
	public static class Pagination {
		/**
		 * 기본 페이지 크기
		 */
		private int defaultSize = 20;

		/**
		 * 최대 페이지 크기
		 */
		private int maxSize = 100;

		/**
		 * 점주용 주문 목록 최대 크기
		 */
		private int storeOrderMaxSize = 100;

		/**
		 * 고객용 주문 목록 최대 크기
		 */
		private int customerOrderMaxSize = 50;
	}

	@Data
	public static class Validation {
		/**
		 * 최대 재고 수량
		 */
		private int maxStockQuantity = 100;

		/**
		 * 최소 주문 금액
		 */
		private int minOrderAmount = 100;

		/**
		 * 최대 주문 금액
		 */
		private int maxOrderAmount = 1000000;
	}

	@Data
	public static class Cache {
		/**
		 * 주문 상세 캐시 TTL (기본: 5분)
		 */
		private Duration orderDetailTtl = Duration.ofMinutes(5);

		/**
		 * 주문 목록 캐시 TTL (기본: 2분)
		 */
		private Duration orderListTtl = Duration.ofMinutes(2);

		/**
		 * 고객 타임라인 캐시 TTL (기본: 3분)
		 */
		private Duration customerTimelineTtl = Duration.ofMinutes(3);
	}

	@Data
	public static class Async {
		/**
		 * 비동기 작업 타임아웃 (기본: 10초)
		 */
		private Duration taskTimeout = Duration.ofSeconds(10);

		/**
		 * 이벤트 발행 재시도 횟수
		 */
		private int eventRetryCount = 3;

		/**
		 * 검증 작업 병렬 처리 여부
		 */
		private boolean parallelValidation = true;
	}
}