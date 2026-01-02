package com.popcorn.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.domain.order.service.OrderDomainService;
import com.popcorn.demo.domain.order.service.OrderItemPriceService;
import com.popcorn.demo.domain.order.repository.OrderRepository;

/**
 * 메인 애플리케이션 테스트
 *
 * 전체 애플리케이션의 기본 테스트를 실행하고
 * 각 도메인별 테스트를 중첩 클래스로 포함합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
public class DemoApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		// 전체 애플리케이션 컨텍스트가 정상적으로 로딩되는지 확인
		assertThat(context).isNotNull();
	}

	/**
	 * 주문 도메인 테스트 - 중첩 클래스로 구성
	 */
	@Nested
	@SpringBootTest
	@ActiveProfiles("test")
	class OrderDomainTests {

		@Autowired
		private OrderService orderService;

		@Autowired
		private OrderDomainService orderDomainService;

		@Autowired
		private OrderItemPriceService orderItemPriceService;

		@Autowired
		private OrderRepository orderRepository;

		@Test
		void contextLoads() {
			// 주문 도메인 관련 컨텍스트가 정상적으로 로딩되는지 확인
			assertThat(orderService).isNotNull();
		}

		@Test
		void orderServiceBeansLoaded() {
			// 주문 관련 서비스 빈들이 정상적으로 로딩되는지 확인
			assertThat(orderService).isNotNull();
			assertThat(orderDomainService).isNotNull();
			assertThat(orderItemPriceService).isNotNull();
		}

		@Test
		void orderRepositoryBeansLoaded() {
			// 주문 관련 레포지토리 빈들이 정상적으로 로딩되는지 확인
			assertThat(orderRepository).isNotNull();
		}
	}
}