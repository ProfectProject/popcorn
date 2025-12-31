package com.popcorn.demo.infrastructure.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import com.popcorn.demo.application.order.port.out.FindOrderItemPricePort;
import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.NotifyOrderPort;
import com.popcorn.demo.application.order.port.out.ProcessOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.application.order.usecase.CreateOrderUseCase;
import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.service.OrderDomainService;
import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.infrastructure.external.async.OrderAdapter;
import com.popcorn.demo.infrastructure.external.notification.OrderNotificationAdapter;
import com.popcorn.demo.infrastructure.persistence.order.OrderRepositoryAdapter;

/**

	* Infrastructure Layer DI 설정 (Clean Architecture)

	*

	* Clean Architecture의 의존성 역전 원칙을 구현합니다:

	* - Application Layer의 Port(인터페이스)를 Infrastructure Layer의 Adapter(구현체)로 연결

	* - 외부 시스템과의 연동을 담당하는 Adapter들을 Bean으로 등록

	* - JPA Repository와 비동기 처리 활성화

	*

	* Port-Adapter 패턴:

	* - Port: Application Layer에서 정의한 인터페이스 (의존성 역전)

	* - Adapter: Infrastructure Layer에서 구현한 실제 구현체

	*/

@Configuration

@ComponentScan(basePackages = {

	"com.popcorn.demo.infrastructure.persistence",

	"com.popcorn.demo.infrastructure.external",

	"com.popcorn.demo.application.order",

	"com.popcorn.demo.domain.order.service"

})

@EnableR2dbcAuditing
@EnableR2dbcRepositories(basePackages = "com.popcorn.demo.infrastructure.persistence.repository")

@EnableAsync

public class InfrastructureConfig {



	/**

		* 주문 도메인 서비스 Bean

		* Clean Architecture의 Domain Layer - 순수 비즈니스 로직

		*/

	@Bean

	public OrderDomainService orderDomainService() {

		return new OrderDomainService();

	}



	/**

		* 주문 조회 Port의 Adapter 구현체

		* Clean Architecture: Application Layer Port → Infrastructure Layer Adapter

		*/

	@Bean

	public FindOrderPort findOrderPort(OrderRepository orderRepository) {

		return new OrderRepositoryAdapter(orderRepository);

	}



	/**

		* 주문 저장 Port의 Adapter 구현체

		* Clean Architecture: Application Layer Port → Infrastructure Layer Adapter

		*/

	@Bean

	public SaveOrderPort saveOrderPort(OrderRepository orderRepository) {

		return new OrderRepositoryAdapter(orderRepository);

	}



	/**

		* 주문 알림 Port의 Adapter 구현체

		* Clean Architecture: Application Layer Port → Infrastructure Layer Adapter

		*/

	@Bean

	public NotifyOrderPort notifyOrderPort() {

		return new OrderNotificationAdapter();

	}



	/**

		* 주문 비동기 처리 Port의 Adapter 구현체

		* Clean Architecture: Application Layer Port → Infrastructure Layer Adapter

		*/

	@Bean

	public ProcessOrderPort processOrderPort(OrderService orderService) {

		return new OrderAdapter(orderService);

	}

	/**

		* 주문 생성 UseCase Bean

		* Clean Architecture: Application Layer UseCase

		*/

	@Bean

	public CreateOrderUseCase createOrderUseCase(

			OrderDomainService orderDomainService,

			FindOrderPort findOrderPort,

			SaveOrderPort saveOrderPort,

			ProcessOrderPort processOrderPort,

			FindOrderItemPricePort findOrderItemPricePort,

			IdempotencyCache idempotencyCache,

			ApplicationEventPublisher eventPublisher) {

		return new CreateOrderUseCase(

				orderDomainService,

				findOrderPort,

				saveOrderPort,

				processOrderPort,

				findOrderItemPricePort,

				idempotencyCache,

				eventPublisher

		);

	}


}
