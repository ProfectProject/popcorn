package com.popcorn.demo.infrastructure.external.async;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.popcorn.demo.domain.order.service.OrderService;

import java.util.UUID;

/**
	* OrderAdapter 단위 테스트
	*
	* Clean Architecture의 Infrastructure Layer 테스트
	* - ProcessOrderPort의 구현체 테스트
	* - 처리 작업의 정상 동작 검증
	*/
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 어댑터 테스트")
class OrderAdapterTest {

	@Mock
	private OrderService orderService;

	private OrderAdapter orderAdapter;

	@BeforeEach
	void setUp() {
		orderAdapter = new OrderAdapter(orderService);
	}

	@Test
	@DisplayName("주문 후처리 작업 - 정상 처리")
	void processOrderPostActions_ValidOrderId_ReturnsMono() {
		// given
		UUID orderId = UUID.randomUUID();
		when(orderService.processOrderPostActions(orderId)).thenReturn(Mono.empty());

		// when
		Mono<Void> result = orderAdapter.processOrderPostActions(orderId);

		// then
		StepVerifier.create(result)
				.verifyComplete();
		verify(orderService, times(1)).processOrderPostActions(orderId);
	}

	@Test
	@DisplayName("주문 후처리 작업 - null 주문 ID로 호출 시 예외 발생")
	void processOrderPostActions_NullOrderId_ThrowsException() {
		// when & then
		StepVerifier.create(orderAdapter.processOrderPostActions(null))
				.expectError(IllegalArgumentException.class)
				.verify();
	}

	@Test
	@DisplayName("주문 검증 처리 - 정상 검증 통과")
	void validateOrder_ValidInput_ReturnsTrue() {
		// given
		Long userId = 1001L;
		UUID productId = UUID.randomUUID();
		int qty = 2;

		when(orderService.validateOrderAsync(userId, productId, qty)).thenReturn(Mono.just(true));

		// when
		Mono<Boolean> result = orderAdapter.validateOrder(userId, productId, qty);

		// then
		StepVerifier.create(result)
				.expectNext(true)
				.verifyComplete();
		verify(orderService, times(1)).validateOrderAsync(userId, productId, qty);
	}

	@Test
	@DisplayName("어댑터 초기화 - OrderService가 null인 경우 예외 발생")
	void constructor_NullService_ThrowsException() {
		// when & then
		assertThatThrownBy(() -> new OrderAdapter(null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
