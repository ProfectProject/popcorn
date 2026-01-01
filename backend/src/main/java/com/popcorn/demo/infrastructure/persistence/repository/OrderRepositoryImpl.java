package com.popcorn.demo.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

/**

	* 도메인 OrderRepository 인터페이스의 R2DBC 구현체

	* - Clean Architecture: 어댑터 패턴 적용

	* - 도메인 레이어의 인터페이스를 인프라스트럭처 레이어에서 구현

	* - R2DBC Repository를 래핑하여 도메인 요구사항 충족

	*/

@Repository("orderRepositoryImpl")

public class OrderRepositoryImpl implements OrderRepository {



	private final R2dbcOrderItemRepository orderItemRepository;
	private final R2dbcOrderRepository orderRepository;
	private final R2dbcOrderStatusHistoryRepository orderStatusHistoryRepository;



	/**

		* 생성자 기반 의존성 주입

		* @param orderRepository R2DBC Repository

		*/

	@Autowired

	public OrderRepositoryImpl(
			R2dbcOrderItemRepository orderItemRepository,
			R2dbcOrderRepository orderRepository,
			R2dbcOrderStatusHistoryRepository orderStatusHistoryRepository) {

		this.orderItemRepository = orderItemRepository;
		this.orderRepository = orderRepository;
		this.orderStatusHistoryRepository = orderStatusHistoryRepository;

	}



	// ========================= 기본 CRUD 메서드 =========================



	@Override

	public Mono<Order> save(Order order) {
		if (order == null) {
			return Mono.empty();
		}
		if (order.getId() == null) {
			return orderRepository.insertOrder(
					order.getOrderNo(),
					order.getCustomerId(),
					order.getStoreId(),
					order.getProductId(),
					order.getOrderType().name(),
					order.getStatus().name(),
					order.getCancelableUntil(),
					order.getTotalAmount(),
					order.getIdempotencyKey(),
					order.getVersion() == null ? 0L : order.getVersion()
			).map(savedOrder -> {
				savedOrder.setOrderItems(order.getOrderItems());
				return savedOrder;
			});
		}
		return orderRepository.updateOrder(
				order.getId(),
				order.getOrderNo(),
				order.getCustomerId(),
				order.getStoreId(),
				order.getProductId(),
				order.getOrderType().name(),
				order.getStatus().name(),
				order.getCancelableUntil(),
				order.getTotalAmount(),
				order.getIdempotencyKey(),
				order.getVersion() == null ? 0L : order.getVersion()
		).map(savedOrder -> {
			savedOrder.setOrderItems(order.getOrderItems());
			return savedOrder;
		});

	}



	@Override

	public Mono<Void> saveOrderItems(List<OrderItem> orderItems) {

		if (orderItems == null || orderItems.isEmpty()) {
			return Mono.empty();
		}
		return Flux.fromIterable(orderItems)
				.flatMap(item -> orderItemRepository.insertOrderItem(
						item.getId(),
						item.getOrderId(),
						item.getOrderItemType().name(),
						item.getSessionOptionId(),
						item.getMerchVariantId(),
						item.getQty(),
						item.getUnitPrice(),
						item.getLineAmount()
				))
				.then();

	}



	@Override

	public Mono<Order> findById(UUID orderId) {

		return orderRepository.findById(orderId);

	}



	@Override

	public Mono<OrderSummaryView> findSummaryById(UUID orderId) {

		return orderRepository.findSummaryById(orderId);

	}



	@Override

	public Mono<Order> findByOrderNo(String orderNo) {

		return orderRepository.findByOrderNo(orderNo);

	}



	@Override

	public Mono<Void> deleteById(UUID orderId) {

		return orderRepository.deleteById(orderId);

	}



	@Override

	public Mono<Boolean> existsById(UUID orderId) {

		return orderRepository.existsById(orderId);

	}



	// ========================= 비즈니스 조회 메서드 =========================



	@Override

	public Flux<Order> findByCustomerId(Long customerId) {

		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

	}



	@Override

	public Flux<Order> findByCustomerId(Long customerId, int offset, int limit) {

		long safeOffset = Math.max(0, offset);
		int safeLimit = Math.max(1, limit);
		return orderRepository.findByCustomerIdWithPaging(customerId, safeOffset, safeLimit);

	}

	@Override
	public Flux<OrderSummaryView> findSummariesByCustomerId(Long customerId, int offset, int limit) {
		long safeOffset = Math.max(0, offset);
		int safeLimit = Math.max(1, limit);
		return orderRepository.findSummariesByCustomerIdWithPaging(customerId, safeOffset, safeLimit);
	}



	@Override

	public Flux<Order> findByStoreId(UUID storeId) {

		return orderRepository.findByStoreId(storeId);

	}



	@Override

	public Flux<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status) {

		return orderRepository.findByStoreIdAndStatus(storeId, status);

	}



	@Override

	public Flux<Order> findByProductId(UUID productId) {

		return orderRepository.findByProductId(productId);

	}



	// ========================= 상태별 조회 메서드 =========================



	@Override

	public Flux<Order> findByStatus(OrderStatus status) {

		return orderRepository.findByStatus(status);

	}



	@Override

	public Flux<Order> findCancelableOrders(LocalDateTime currentTime) {

		return orderRepository.findCancelableOrders(currentTime);

	}



	@Override

	public Flux<Order> findExpiredCancelableOrders(LocalDateTime currentTime) {

		return orderRepository.findExpiredCancelableOrders(currentTime);

	}



	// ========================= 통계 및 집계 메서드 =========================



	@Override

	public Mono<Long> countByCustomerId(Long customerId) {

		return orderRepository.countByCustomerId(customerId);

	}



	@Override

	public Mono<Long> countByStoreId(UUID storeId) {

		return orderRepository.countByStoreId(storeId);

	}



	@Override

	public Mono<Long> countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {

		return orderRepository.countByCreatedAtBetween(startDate, endDate);

	}



	@Override

	public Mono<Long> sumTotalAmountByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime startDate,
			LocalDateTime endDate) {

		return orderRepository.sumTotalAmountByCustomerIdAndCreatedAtBetween(customerId, startDate, endDate);

	}



	// ========================= 중복 방지 메서드 =========================



	@Override

	public Mono<Order> findByIdempotencyKey(String idempotencyKey) {

		return orderRepository.findByIdempotencyKey(idempotencyKey);

	}



	@Override

	public Mono<Boolean> existsByIdempotencyKey(String idempotencyKey) {

		return orderRepository.existsByIdempotencyKey(idempotencyKey);

	}


	@Override
	public Mono<Void> saveStatusHistory(OrderStatusHistory history) {
		if (history == null) {
			return Mono.empty();
		}
		String fromStatus = history.getFromStatus() == null ? null : history.getFromStatus().name();
		return orderStatusHistoryRepository.insertStatusHistory(
				history.getOrderId(),
				fromStatus,
				history.getToStatus().name(),
				history.getReason(),
				history.getChangedAt()
		).then();
	}

}
