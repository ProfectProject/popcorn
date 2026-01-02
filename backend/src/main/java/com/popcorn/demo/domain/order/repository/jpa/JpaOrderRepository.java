package com.popcorn.demo.domain.order.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {

	Optional<Order> findByOrderNo(String orderNo);

	Optional<Order> findByIdempotencyKey(String idempotencyKey);

	boolean existsByIdempotencyKey(String idempotencyKey);

	List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

	@Query(value = "SELECT * FROM p_orders WHERE customer_id = :customerId ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
			nativeQuery = true)
	List<Order> findByCustomerIdWithPaging(@Param("customerId") Long customerId,
			@Param("offset") long offset,
			@Param("limit") int limit);

	List<Order> findByStoreId(UUID storeId);

	List<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status);

	List<Order> findByProductId(UUID productId);

	List<Order> findByStatus(OrderStatus status);

	@Query(value = "SELECT * FROM p_orders WHERE cancelable_until > :currentTime", nativeQuery = true)
	List<Order> findCancelableOrders(@Param("currentTime") LocalDateTime currentTime);

	@Query(value = "SELECT * FROM p_orders WHERE cancelable_until <= :currentTime", nativeQuery = true)
	List<Order> findExpiredCancelableOrders(@Param("currentTime") LocalDateTime currentTime);

	long countByCustomerId(Long customerId);

	long countByStoreId(UUID storeId);

	long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	@Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM p_orders WHERE customer_id = :customerId AND created_at BETWEEN :startDate AND :endDate",
			nativeQuery = true)
	long sumTotalAmountByCustomerIdAndCreatedAtBetween(
			@Param("customerId") Long customerId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query(value = "SELECT id AS id, order_no AS orderNo, status AS status, total_amount AS totalAmount, created_at AS createdAt FROM p_orders WHERE id = :orderId",
			nativeQuery = true)
	OrderSummaryView findSummaryById(@Param("orderId") UUID orderId);

	@Query(value = """
		SELECT id AS id,
		       order_no AS orderNo,
		       status AS status,
		       total_amount AS totalAmount,
		       created_at AS createdAt
		FROM p_orders
		WHERE customer_id = :customerId
		ORDER BY created_at DESC
		LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<OrderSummaryView> findSummariesByCustomerIdWithPaging(
			@Param("customerId") Long customerId,
			@Param("offset") long offset,
			@Param("limit") int limit);
}
