package com.popcorn.demo.infrastructure.external.async;

import java.util.UUID;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import com.popcorn.demo.application.order.port.out.ProcessOrderPort;
import com.popcorn.demo.domain.order.service.OrderService;

/**

	* 주문 처리 어댑터 (Infrastructure Layer)

	*

	* Clean Architecture의 Adapter 패턴을 구현합니다.

	* - Application Layer의 ProcessOrderPort를 구현

	* - 기존 OrderService와 연결하는 브릿지 역할

	* - 작업들을 조율하고 관리

	*/

@Component

public class OrderAdapter implements ProcessOrderPort {



	private final OrderService orderService;



	public OrderAdapter(OrderService orderService) {

		if (orderService == null) {

			throw new IllegalArgumentException("OrderService cannot be null");

		}

		this.orderService = orderService;

	}



	@Override

	public Mono<Void> processOrderPostActions(UUID orderId) {
		if (orderId == null) {
			return Mono.error(new IllegalArgumentException("Order ID cannot be null"));
		}
		return orderService.processOrderPostActions(orderId);

	}



	@Override

	public Mono<Boolean> validateOrder(Long userId, UUID productId, Integer quantity) {

		return orderService.validateOrderAsync(userId, productId, quantity);

	}



	@Override

	public Mono<Boolean> deductStock(UUID orderId) {

		return orderService.processOrderPostActions(orderId)
				.thenReturn(true)
				.onErrorReturn(false);

	}



	@Override

	public Mono<Boolean> processPayment(UUID orderId, String paymentInfo) {

		return orderService.processOrderPostActions(orderId)
				.thenReturn(true)
				.onErrorReturn(false);

	}

}
