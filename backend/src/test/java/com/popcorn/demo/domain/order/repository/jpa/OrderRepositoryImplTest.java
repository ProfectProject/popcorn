package com.popcorn.demo.domain.order.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryImplTest {

	@Mock
	private JpaOrderRepository orderRepository;

	@Mock
	private JpaOrderItemRepository orderItemRepository;

	@Mock
	private JpaOrderStatusHistoryRepository orderStatusHistoryRepository;

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
	@DisplayName("Save - JPA save 호출 후 결과 반환")
	void save_DelegatesToJpaSave() {
		Order order = buildOrder("O-1001", 1001L);
		when(orderRepository.save(order)).thenReturn(order);

		Order result = orderRepositoryImpl.save(order);

		assertThat(result.getOrderNo()).isEqualTo("O-1001");
		verify(orderRepository).save(order);
	}

	@Test
	@DisplayName("SaveOrderItems - saveAll 호출 후 완료")
	void saveOrderItems_SavesAll() {
		List<OrderItem> items = List.of(
				OrderItem.builder()
						.id(UUID.randomUUID())
						.orderId(UUID.randomUUID())
						.qty(1)
						.unitPrice(1000)
						.lineAmount(1000)
						.build()
		);
		when(orderItemRepository.saveAll(items)).thenReturn(items);

		orderRepositoryImpl.saveOrderItems(items);
		verify(orderItemRepository).saveAll(items);
	}

	@Test
	@DisplayName("FindById - Optional 결과 반환")
	void findById_ReturnsOptional() {
		UUID orderId = UUID.randomUUID();
		Order order = buildOrder("O-2001", 2001L);
		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

		Optional<Order> result = orderRepositoryImpl.findById(orderId);
		assertThat(result).isPresent();
		assertThat(result.get().getOrderNo()).isEqualTo("O-2001");
	}

	@Test
	@DisplayName("FindSummaryById - 요약 뷰 반환")
	void findSummaryById_ReturnsSummary() {
		UUID orderId = UUID.randomUUID();
		OrderSummaryView summary = new OrderSummaryView() {
			@Override
			public UUID getId() {
				return orderId;
			}

			@Override
			public String getOrderNo() {
				return "O-3001";
			}

			@Override
			public String getStatus() {
				return OrderStatus.REQUESTED.name();
			}

			@Override
			public Integer getTotalAmount() {
				return 1200;
			}

			@Override
			public LocalDateTime getCreatedAt() {
				return LocalDateTime.now();
			}
		};
		when(orderRepository.findSummaryById(orderId)).thenReturn(summary);

		Optional<OrderSummaryView> result = orderRepositoryImpl.findSummaryById(orderId);
		assertThat(result).isPresent();
		assertThat(result.get().getOrderNo()).isEqualTo("O-3001");
	}

	@Test
	@DisplayName("FindByOrderNo - Optional 결과 반환")
	void findByOrderNo_ReturnsOrder() {
		Order order = buildOrder("O-4001", 4001L);
		when(orderRepository.findByOrderNo("O-4001")).thenReturn(Optional.of(order));

		Optional<Order> result = orderRepositoryImpl.findByOrderNo("O-4001");
		assertThat(result).isPresent();
		assertThat(result.get().getCustomerId()).isEqualTo(4001L);
	}

	@Test
	@DisplayName("FindByCustomerId - 리스트 조회")
	void findByCustomerId_ReturnsList() {
		Long customerId = 1001L;
		when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
				.thenReturn(List.of(buildOrder("O-5001", customerId)));

		List<Order> result = orderRepositoryImpl.findByCustomerId(customerId);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOrderNo()).isEqualTo("O-5001");
	}

	@Test
	@DisplayName("CountByCustomerId - 카운트 반환")
	void countByCustomerId_ReturnsCount() {
		when(orderRepository.countByCustomerId(1001L)).thenReturn(3L);

		long result = orderRepositoryImpl.countByCustomerId(1001L);
		assertThat(result).isEqualTo(3L);
	}

	@Test
	@DisplayName("SumTotalAmount - 합계 반환")
	void sumTotalAmount_ReturnsValue() {
		when(orderRepository.sumTotalAmountByCustomerIdAndCreatedAtBetween(any(), any(), any()))
				.thenReturn(9000L);

		long result = orderRepositoryImpl.sumTotalAmountByCustomerIdAndCreatedAtBetween(
				1001L,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now());
		assertThat(result).isEqualTo(9000L);
	}

	@Test
	@DisplayName("FindByIdempotencyKey - Optional 결과 반환")
	void findByIdempotencyKey_ReturnsOrder() {
		Order order = buildOrder("O-6001", 6001L);
		when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(order));

		Optional<Order> result = orderRepositoryImpl.findByIdempotencyKey("key-1");
		assertThat(result).isPresent();
		assertThat(result.get().getOrderNo()).isEqualTo("O-6001");
	}

	@Test
	@DisplayName("SaveStatusHistory - 이력 저장")
	void saveStatusHistory_Saves() {
		OrderStatusHistory history = OrderStatusHistory.builder()
				.orderId(UUID.randomUUID())
				.fromStatus(OrderStatus.REQUESTED)
				.toStatus(OrderStatus.OWNER_ACCEPTED)
				.reason("approved")
				.changedAt(LocalDateTime.now())
				.build();
		when(orderStatusHistoryRepository.save(history)).thenReturn(history);

		orderRepositoryImpl.saveStatusHistory(history);
		verify(orderStatusHistoryRepository).save(history);
	}

	private Order buildOrder(String orderNo, Long customerId) {
		return Order.builder()
				.id(UUID.randomUUID())
				.orderNo(orderNo)
				.customerId(customerId)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(1000)
				.cancelableUntil(LocalDateTime.now().plusMinutes(5))
				.build();
	}
}
