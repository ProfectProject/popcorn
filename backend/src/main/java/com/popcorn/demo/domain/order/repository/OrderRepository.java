package com.popcorn.demo.domain.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;

/**
 * 주문 영속성 인터페이스 (도메인 계층).
 * - JPA 구현체가 이 인터페이스를 구현합니다.
 * - 동기 방식으로 동작합니다.
 */
public interface OrderRepository {

	Optional<Order> findByIdempotencyKey(String idempotencyKey);

	Optional<Order> findById(UUID orderId);

	Optional<OrderSummaryView> findSummaryById(UUID orderId);

	Optional<Order> findByOrderNo(String orderNo);

	Order save(Order order);

	void saveOrderItems(List<OrderItem> orderItems);

	void saveStatusHistory(OrderStatusHistory history);

	void deleteById(UUID orderId);

	boolean existsById(UUID orderId);

	boolean existsByIdempotencyKey(String idempotencyKey);

	List<Order> findByCustomerId(Long customerId);

	List<Order> findByCustomerId(Long customerId, int offset, int limit);

	List<OrderSummaryView> findSummariesByCustomerId(Long customerId, int offset, int limit);

	List<Order> findByStoreId(UUID storeId);

	List<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status);

	List<Order> findByProductId(UUID productId);

	List<Order> findByStatus(OrderStatus status);

	List<Order> findCancelableOrders(LocalDateTime currentTime);

	List<Order> findExpiredCancelableOrders(LocalDateTime currentTime);

	long countByCustomerId(Long customerId);

	long countByStoreId(UUID storeId);

	long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	long sumTotalAmountByCustomerIdAndCreatedAtBetween(
			Long customerId, LocalDateTime startDate, LocalDateTime endDate);
}
