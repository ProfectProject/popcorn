package com.popcorn.demo.domain.order.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

class OrderRepositoryImplTest {

	@Test
	@DisplayName("Order 저장은 null 입력을 허용한다")
	void saveHandlesNullOrder() {
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				Mockito.mock(JpaOrderRepository.class),
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		assertThat(repository.save(null)).isNull();
	}

	@Test
	@DisplayName("Order 저장은 아이템을 유지한다")
	void savePreservesOrderItems() {
		JpaOrderRepository jpaOrderRepository = Mockito.mock(JpaOrderRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				jpaOrderRepository,
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		OrderItem item = OrderItem.builder().id(UUID.randomUUID()).build();
		Order order = Order.builder().id(UUID.randomUUID()).orderItems(List.of(item)).build();
		when(jpaOrderRepository.save(order)).thenReturn(order);

		Order saved = repository.save(order);

		assertThat(saved.getOrderItems()).containsExactly(item);
	}

	@Test
	@DisplayName("OrderItem 저장은 빈 목록을 무시한다")
	void saveOrderItemsSkipsEmptyList() {
		JpaOrderItemRepository itemRepository = Mockito.mock(JpaOrderItemRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				itemRepository,
				Mockito.mock(JpaOrderRepository.class),
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		repository.saveOrderItems(List.of());

		verify(itemRepository, never()).saveAll(Mockito.anyList());
	}

	@Test
	@DisplayName("Summary 조회는 Optional로 감싼다")
	void findSummaryByIdWrapsOptional() {
		JpaOrderRepository jpaOrderRepository = Mockito.mock(JpaOrderRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				jpaOrderRepository,
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		OrderSummaryView view = Mockito.mock(OrderSummaryView.class);
		UUID orderId = UUID.randomUUID();
		when(jpaOrderRepository.findSummaryById(orderId)).thenReturn(view);

		Optional<OrderSummaryView> summary = repository.findSummaryById(orderId);
		assertThat(summary).contains(view);
	}

	@Test
	@DisplayName("기본 조회 메서드는 JPA 저장소에 위임한다")
	void basicQueriesDelegate() {
		JpaOrderRepository jpaOrderRepository = Mockito.mock(JpaOrderRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				jpaOrderRepository,
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		UUID orderId = UUID.randomUUID();
		Order order = Order.builder().id(orderId).build();
		when(jpaOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(jpaOrderRepository.findByOrderNo("O-1")).thenReturn(Optional.of(order));
		when(jpaOrderRepository.existsById(orderId)).thenReturn(true);

		assertThat(repository.findById(orderId)).contains(order);
		assertThat(repository.findByOrderNo("O-1")).contains(order);
		assertThat(repository.existsById(orderId)).isTrue();

		repository.deleteById(orderId);
		verify(jpaOrderRepository).deleteById(orderId);
	}

	@Test
	@DisplayName("비즈니스 조회/카운트 메서드를 위임한다")
	void businessQueriesDelegate() {
		JpaOrderRepository jpaOrderRepository = Mockito.mock(JpaOrderRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				jpaOrderRepository,
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		UUID storeId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		when(jpaOrderRepository.findByCustomerIdOrderByCreatedAtDesc(1001L)).thenReturn(List.of());
		when(jpaOrderRepository.findByStoreId(storeId)).thenReturn(List.of());
		when(jpaOrderRepository.findByStoreIdAndStatus(storeId, OrderStatus.REQUESTED)).thenReturn(List.of());
		when(jpaOrderRepository.findByStatus(OrderStatus.REQUESTED)).thenReturn(List.of());
		when(jpaOrderRepository.findCancelableOrders(now)).thenReturn(List.of());
		when(jpaOrderRepository.findExpiredCancelableOrders(now)).thenReturn(List.of());
		when(jpaOrderRepository.countByCustomerId(1001L)).thenReturn(1L);
		when(jpaOrderRepository.countByStoreId(storeId)).thenReturn(2L);
		when(jpaOrderRepository.countByCreatedAtBetween(now.minusDays(1), now)).thenReturn(3L);
		when(jpaOrderRepository.sumTotalAmountByCustomerIdAndCreatedAtBetween(1001L, now.minusDays(1), now))
				.thenReturn(10000L);

		repository.findByCustomerId(1001L);
		repository.findByStoreId(storeId);
		repository.findByStoreIdAndStatus(storeId, OrderStatus.REQUESTED);
		repository.findByStatus(OrderStatus.REQUESTED);
		repository.findCancelableOrders(now);
		repository.findExpiredCancelableOrders(now);
		assertThat(repository.countByCustomerId(1001L)).isEqualTo(1L);
		assertThat(repository.countByStoreId(storeId)).isEqualTo(2L);
		assertThat(repository.countByCreatedAtBetween(now.minusDays(1), now)).isEqualTo(3L);
		assertThat(repository.sumTotalAmountByCustomerIdAndCreatedAtBetween(1001L, now.minusDays(1), now))
				.isEqualTo(10000L);
	}

	@Test
	@DisplayName("페이징 조회는 offset/limit을 보정한다")
	void pagingAdjustsNegativeValues() {
		JpaOrderRepository jpaOrderRepository = Mockito.mock(JpaOrderRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				jpaOrderRepository,
				Mockito.mock(JpaOrderStatusHistoryRepository.class));

		when(jpaOrderRepository.findByCustomerIdWithPaging(1001L, 0L, 1)).thenReturn(List.of());
		when(jpaOrderRepository.findSummariesByCustomerIdWithPaging(1001L, 0L, 1)).thenReturn(List.of());

		repository.findByCustomerId(1001L, -1, -10);
		repository.findSummariesByCustomerId(1001L, -5, 0);

		verify(jpaOrderRepository).findByCustomerIdWithPaging(1001L, 0L, 1);
		verify(jpaOrderRepository).findSummariesByCustomerIdWithPaging(1001L, 0L, 1);
	}

	@Test
	@DisplayName("deleteAllOrders는 순서대로 삭제한다")
	void deleteAllOrdersDeletesInOrder() {
		JpaOrderItemRepository itemRepository = Mockito.mock(JpaOrderItemRepository.class);
		JpaOrderRepository orderRepository = Mockito.mock(JpaOrderRepository.class);
		JpaOrderStatusHistoryRepository historyRepository = Mockito.mock(JpaOrderStatusHistoryRepository.class);

		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				itemRepository,
				orderRepository,
				historyRepository);

		repository.deleteAllOrders();

		verify(historyRepository).deleteAll();
		verify(itemRepository).deleteAll();
		verify(orderRepository).deleteAll();
	}

	@Test
	@DisplayName("상태 이력 저장은 JPA 저장소에 위임한다")
	void saveStatusHistoryDelegates() {
		JpaOrderStatusHistoryRepository historyRepository = Mockito.mock(JpaOrderStatusHistoryRepository.class);
		OrderRepositoryImpl repository = new OrderRepositoryImpl(
				Mockito.mock(JpaOrderItemRepository.class),
				Mockito.mock(JpaOrderRepository.class),
				historyRepository);

		OrderStatusHistory history = OrderStatusHistory.builder()
				.orderId(UUID.randomUUID())
				.fromStatus(OrderStatus.REQUESTED)
				.toStatus(OrderStatus.ACCEPTED)
				.changedAt(LocalDateTime.now())
				.build();

		repository.saveStatusHistory(history);

		verify(historyRepository).save(history);
	}
}
