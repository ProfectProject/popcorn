package com.popcorn.demo.infrastructure.external.async;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

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
	public CompletableFuture<Void> processOrderPostActions(Long orderId) {
		if (orderId == null || orderId <= 0) {
			throw new IllegalArgumentException("Order ID must be positive");
		}
		return orderService.processOrderPostActions(orderId);
	}

	@Override
	public CompletableFuture<Boolean> validateOrder(Long userId, Long productId, Integer quantity) {
		return orderService.validateOrderAsync(userId, productId, quantity);
	}

	@Override
	public CompletableFuture<Boolean> deductStock(Long orderId) {
		// 재고 차감 로직을 OrderService를 통해 실행
		return orderService.processOrderPostActions(orderId)
				.thenApply(result -> {
					// 후처리 작업이 성공했다면 재고 차감도 성공한 것으로 간주
					return true;
				})
				.exceptionally(throwable -> {
					// 실패 시 false 반환
					return false;
				});
	}

	@Override
	public CompletableFuture<Boolean> processPayment(Long orderId, String paymentInfo) {
		// 결제 처리는 향후 구현 예정
		// 현재는 기본 후처리 작업을 통해 처리
		return orderService.processOrderPostActions(orderId)
				.thenApply(result -> true)
				.exceptionally(throwable -> false);
	}
}