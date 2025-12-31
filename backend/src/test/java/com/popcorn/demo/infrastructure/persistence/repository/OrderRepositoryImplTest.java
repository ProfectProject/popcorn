package com.popcorn.demo.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryImplTest {

	@Mock
	private R2dbcOrderRepository orderRepository;

	@Mock
	private R2dbcOrderItemRepository orderItemRepository;

	@Mock
	private R2dbcOrderStatusHistoryRepository orderStatusHistoryRepository;

	private OrderRepositoryImpl orderRepositoryImpl;

	@BeforeEach
	void setUp() {
		orderRepositoryImpl = new OrderRepositoryImpl(
				orderItemRepository,
				orderRepository,
				orderStatusHistoryRepository
		);
	}

	@Test
	@DisplayName("Save - 신규 주문이면 insertOrder 호출")
	void save_NewOrder_UsesInsertOrder() {
		// given
		Order order = buildOrder("O-1001", 1001L, UUID.randomUUID(), UUID.randomUUID(), 29000);
		UUID savedOrderId = UUID.randomUUID();
		Order savedOrder = Order.builder()
				.id(savedOrderId)
				.orderNo(order.getOrderNo())
				.customerId(order.getCustomerId())
				.storeId(order.getStoreId())
				.productId(order.getProductId())
				.orderType(order.getOrderType())
				.status(order.getStatus())
				.totalAmount(order.getTotalAmount())
				.build();

		when(orderRepository.insertOrder(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
		)).thenReturn(Mono.just(savedOrder));

		// when
		Mono<Order> result = orderRepositoryImpl.save(order);

		// then
		StepVerifier.create(result)
				.assertNext(saved -> assertThat(saved.getId()).isEqualTo(savedOrderId))
				.verifyComplete();
		verify(orderRepository).insertOrder(
				order.getOrderNo(),
				order.getCustomerId(),
				order.getStoreId(),
				order.getProductId(),
				order.getOrderType().name(),
				order.getStatus().name(),
				order.getCancelableUntil(),
				order.getTotalAmount(),
				order.getIdempotencyKey(),
				0L
		);
		verify(orderRepository, never()).updateOrder(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
		);
	}

	@Test
	@DisplayName("Save - 기존 주문이면 updateOrder 호출")
	void save_ExistingOrder_UsesUpdateOrder() {
		// given
		UUID orderId = UUID.randomUUID();
		Order baseOrder = buildOrder("O-1002", 1002L, UUID.randomUUID(), UUID.randomUUID(), 31000);
		Order order = Order.builder()
				.id(orderId)
				.orderNo(baseOrder.getOrderNo())
				.customerId(baseOrder.getCustomerId())
				.storeId(baseOrder.getStoreId())
				.productId(baseOrder.getProductId())
				.orderType(baseOrder.getOrderType())
				.status(baseOrder.getStatus())
				.totalAmount(baseOrder.getTotalAmount())
				.idempotencyKey("idemp-001")
				.build();

		when(orderRepository.updateOrder(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
		)).thenReturn(Mono.just(order));

		// when
		Mono<Order> result = orderRepositoryImpl.save(order);

		// then
		StepVerifier.create(result)
				.assertNext(saved -> assertThat(saved.getId()).isEqualTo(orderId))
				.verifyComplete();
		verify(orderRepository).updateOrder(
				orderId,
				order.getOrderNo(),
				order.getCustomerId(),
				order.getStoreId(),
				order.getProductId(),
				order.getOrderType().name(),
				order.getStatus().name(),
				order.getCancelableUntil(),
				order.getTotalAmount(),
				order.getIdempotencyKey(),
				0L
		);
		verify(orderRepository, never()).insertOrder(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
		);
	}

	@Test
	@DisplayName("FindById - R2dbcOrderRepository delegate")
	void findById_DelegatesToOrderRepository() {
		// given
		UUID orderId = UUID.randomUUID();
		Order order = buildOrder("O-1001", 1001L, UUID.randomUUID(), UUID.randomUUID(), 29000);
		when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

		// when
		Mono<Order> result = orderRepositoryImpl.findById(orderId);

		// then
		StepVerifier.create(result)
				.assertNext(found -> assertThat(found.getOrderNo()).isEqualTo("O-1001"))
				.verifyComplete();
		verify(orderRepository).findById(orderId);
	}

	@Test
	@DisplayName("FindByOrderNo - R2dbcOrderRepository delegate")
	void findByOrderNo_DelegatesToOrderRepository() {
		// given
		Order order = buildOrder("O-2001", 1002L, UUID.randomUUID(), UUID.randomUUID(), 15000);
		when(orderRepository.findByOrderNo("O-2001")).thenReturn(Mono.just(order));

		// when
		Mono<Order> result = orderRepositoryImpl.findByOrderNo("O-2001");

		// then
		StepVerifier.create(result)
				.expectNext(order)
				.verifyComplete();
		verify(orderRepository).findByOrderNo("O-2001");
	}

	@Test
	@DisplayName("ExistsById - R2dbcOrderRepository delegate")
	void existsById_DelegatesToOrderRepository() {
		// given
		UUID orderId = UUID.randomUUID();
		when(orderRepository.existsById(orderId)).thenReturn(Mono.just(false));

		// when
		Mono<Boolean> result = orderRepositoryImpl.existsById(orderId);

		// then
		StepVerifier.create(result)
				.expectNext(false)
				.verifyComplete();
		verify(orderRepository).existsById(orderId);
	}

	@Test
	@DisplayName("DeleteById - R2dbcOrderRepository delegate")
	void deleteById_DelegatesToOrderRepository() {
		// given
		UUID orderId = UUID.randomUUID();
		when(orderRepository.deleteById(orderId)).thenReturn(Mono.empty());

		// when
		Mono<Void> result = orderRepositoryImpl.deleteById(orderId);

		// then
		StepVerifier.create(result)
				.verifyComplete();
		verify(orderRepository).deleteById(orderId);
	}

	@Test
	@DisplayName("SaveOrderItems - insertOrderItem 호출")
	void saveOrderItems_InsertsItems() {
		// given
		UUID orderId = UUID.randomUUID();
		OrderItem item = OrderItem.builder()
				.orderId(orderId)
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(UUID.randomUUID())
				.qty(2)
				.unitPrice(1000)
				.lineAmount(2000)
				.build();

		when(orderItemRepository.insertOrderItem(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
		)).thenReturn(Mono.just(item));

		// when
		Mono<Void> result = orderRepositoryImpl.saveOrderItems(List.of(item));

		// then
		StepVerifier.create(result)
				.verifyComplete();
		verify(orderItemRepository).insertOrderItem(
				orderId,
				OrderItemType.RESERVATION.name(),
				item.getSessionOptionId(),
				item.getMerchVariantId(),
				item.getQty(),
				item.getUnitPrice(),
				item.getLineAmount()
		);
	}

	@Test
	@DisplayName("SaveStatusHistory - insertStatusHistory 호출")
	void saveStatusHistory_InsertsHistory() {
		// given
		UUID orderId = UUID.randomUUID();
		LocalDateTime changedAt = LocalDateTime.now();
		OrderStatusHistory history = OrderStatusHistory.builder()
				.orderId(orderId)
				.fromStatus(OrderStatus.REQUESTED)
				.toStatus(OrderStatus.OWNER_ACCEPTED)
				.reason("승인")
				.changedAt(changedAt)
				.build();

		when(orderStatusHistoryRepository.insertStatusHistory(
				any(),
				any(),
				any(),
				any(),
				any()
		)).thenReturn(Mono.just(history));

		// when
		Mono<Void> result = orderRepositoryImpl.saveStatusHistory(history);

		// then
		StepVerifier.create(result)
				.verifyComplete();
		verify(orderStatusHistoryRepository).insertStatusHistory(
				orderId,
				OrderStatus.REQUESTED.name(),
				OrderStatus.OWNER_ACCEPTED.name(),
				history.getReason(),
				changedAt
		);
	}

	private Order buildOrder(String orderNo, Long customerId, UUID storeId, UUID productId, int totalAmount) {
		return Order.builder()
				.orderNo(orderNo)
				.customerId(customerId)
				.storeId(storeId)
				.productId(productId)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(totalAmount)
				.build();
	}
}
