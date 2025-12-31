package com.popcorn.demo.infrastructure.persistence.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.repository.OrderRepository;

/**
	* OrderRepositoryAdapter 단위 테스트
	*
	* Clean Architecture의 Infrastructure Layer 테스트
	* - Port(인터페이스)와 Repository(구현체) 간의 어댑터 로직 검증
	* - 외부 의존성을 Mock으로 대체하여 격리된 테스트 수행
	*/
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 Repository 어댑터 테스트")
class OrderRepositoryAdapterTest {

	@Mock
	private OrderRepository orderRepository;

	private OrderRepositoryAdapter orderRepositoryAdapter;

	@BeforeEach
	void setUp() {
		orderRepositoryAdapter = new OrderRepositoryAdapter(orderRepository);
	}

	@Test
	@DisplayName("멱등성 키로 주문 조회 - 기존 주문이 있는 경우")
	void findByIdempotencyKey_ExistingOrder_ReturnsOrder() {
		// given
		String idempotencyKey = "test-key-001";
		Order mockOrder = createMockOrder();
		when(orderRepository.findByIdempotencyKey(idempotencyKey))
				.thenReturn(Mono.just(mockOrder));

		// when
		Mono<Order> result = orderRepositoryAdapter.findByIdempotencyKey(idempotencyKey);

		// then
		StepVerifier.create(result)
				.assertNext(found -> {
					assertThat(found.getId()).isEqualTo(mockOrder.getId());
					assertThat(found.getIdempotencyKey()).isEqualTo(idempotencyKey);
				})
				.verifyComplete();

		verify(orderRepository, times(1)).findByIdempotencyKey(idempotencyKey);
	}

	@Test
	@DisplayName("멱등성 키로 주문 조회 - 주문이 없는 경우")
	void findByIdempotencyKey_NoOrder_ReturnsEmpty() {
		// given
		String idempotencyKey = "nonexistent-key";
		when(orderRepository.findByIdempotencyKey(idempotencyKey))
				.thenReturn(Mono.empty());

		// when
		Mono<Order> result = orderRepositoryAdapter.findByIdempotencyKey(idempotencyKey);

		// then
		StepVerifier.create(result)
				.verifyComplete();

		verify(orderRepository, times(1)).findByIdempotencyKey(idempotencyKey);
	}

	@Test
	@DisplayName("ID로 주문 조회 - 기존 주문이 있는 경우")
	void findById_ExistingOrder_ReturnsOrder() {
		// given
		UUID orderId = UUID.randomUUID();
		Order mockOrder = Order.builder()
				.id(orderId)
				.orderNo("O-1001")
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.idempotencyKey("test-key-001")
				.cancelableUntil(LocalDateTime.now().plusDays(1))
				.build();

		when(orderRepository.findById(orderId))
				.thenReturn(Mono.just(mockOrder));

		// when
		Mono<Order> result = orderRepositoryAdapter.findById(orderId);

		// then
		StepVerifier.create(result)
				.assertNext(found -> assertThat(found.getId()).isEqualTo(orderId))
				.verifyComplete();

		verify(orderRepository, times(1)).findById(orderId);
	}

	@Test
	@DisplayName("ID로 주문 조회 - 주문이 없는 경우")
	void findById_NoOrder_ReturnsEmpty() {
		// given
		UUID orderId = UUID.randomUUID();
		when(orderRepository.findById(orderId))
				.thenReturn(Mono.empty());

		// when
		Mono<Order> result = orderRepositoryAdapter.findById(orderId);

		// then
		StepVerifier.create(result)
				.verifyComplete();

		verify(orderRepository, times(1)).findById(orderId);
	}

	@Test
	@DisplayName("주문 저장 - 새로운 주문 저장")
	void save_NewOrder_ReturnsSavedOrder() {
		// given
		Order newOrder = createMockOrder();
		Order savedOrder = Order.builder()
				.id(UUID.randomUUID())
				.orderNo(newOrder.getOrderNo())
				.customerId(newOrder.getCustomerId())
				.storeId(newOrder.getStoreId())
				.productId(newOrder.getProductId())
				.orderType(newOrder.getOrderType())
				.status(newOrder.getStatus())
				.totalAmount(newOrder.getTotalAmount())
				.idempotencyKey(newOrder.getIdempotencyKey())
				.cancelableUntil(newOrder.getCancelableUntil())
				.build();

		when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));

		// when
		Mono<Order> result = orderRepositoryAdapter.save(newOrder);

		// then
		StepVerifier.create(result)
				.assertNext(found -> {
					assertThat(found.getId()).isEqualTo(savedOrder.getId());
					assertThat(found.getOrderNo()).isEqualTo(newOrder.getOrderNo());
					assertThat(found.getCustomerId()).isEqualTo(newOrder.getCustomerId());
				})
				.verifyComplete();

		verify(orderRepository, times(1)).save(newOrder);
	}

	private Order createMockOrder() {
		return Order.builder()
				.id(UUID.randomUUID())
				.orderNo("O-1001")
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(15000)
				.idempotencyKey("test-key-001")
				.cancelableUntil(LocalDateTime.now().plusDays(1))
				.build();
	}
}
