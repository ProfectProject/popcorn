package com.popcorn.demo.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

public interface R2dbcOrderRepository extends ReactiveCrudRepository<Order, UUID> {

	Mono<Order> findByOrderNo(String orderNo);

	Mono<Order> findByIdempotencyKey(String idempotencyKey);

	Mono<Boolean> existsByIdempotencyKey(String idempotencyKey);

	Flux<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

	@Query("SELECT * FROM p_orders WHERE customer_id = :customerId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
	Flux<Order> findByCustomerIdWithPaging(@Param("customerId") Long customerId,
			@Param("offset") long offset,
			@Param("limit") int limit);

	Flux<Order> findByStoreId(UUID storeId);

	Flux<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status);

	Flux<Order> findByProductId(UUID productId);

	Flux<Order> findByStatus(OrderStatus status);

	@Query("SELECT * FROM p_orders WHERE cancelable_until > :currentTime")
	Flux<Order> findCancelableOrders(@Param("currentTime") LocalDateTime currentTime);

	@Query("SELECT * FROM p_orders WHERE cancelable_until <= :currentTime")
	Flux<Order> findExpiredCancelableOrders(@Param("currentTime") LocalDateTime currentTime);

	Mono<Long> countByCustomerId(Long customerId);

	Mono<Long> countByStoreId(UUID storeId);

	Mono<Long> countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	@Query("SELECT COALESCE(SUM(total_amount), 0) FROM p_orders WHERE customer_id = :customerId AND created_at BETWEEN :startDate AND :endDate")
	Mono<Long> sumTotalAmountByCustomerIdAndCreatedAtBetween(
			@Param("customerId") Long customerId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT id AS id, order_no AS orderNo, status AS status, total_amount AS totalAmount, created_at AS createdAt FROM p_orders WHERE id = :orderId")
	Mono<OrderSummaryView> findSummaryById(@Param("orderId") UUID orderId);

	@Query("""
		SELECT id AS id,
		       order_no AS orderNo,
		       status AS status,
		       total_amount AS totalAmount,
		       created_at AS createdAt
		FROM p_orders
		WHERE customer_id = :customerId
		ORDER BY created_at DESC
		LIMIT :limit OFFSET :offset
		""")
	Flux<OrderSummaryView> findSummariesByCustomerIdWithPaging(
			@Param("customerId") Long customerId,
			@Param("offset") long offset,
			@Param("limit") int limit);

	@Query("""
		INSERT INTO p_orders (
			order_no,
			customer_id,
			store_id,
			product_id,
			order_type,
			status,
			cancelable_until,
			total_amount,
			idempotency_key,
			version,
			created_at,
			updated_at
		) VALUES (
			:orderNo,
			:customerId,
			:storeId,
			:productId,
			CAST(:orderType AS order_type),
			CAST(:status AS order_status),
			:cancelableUntil,
			:totalAmount,
			:idempotencyKey,
			:version,
			NOW(),
			NOW()
		)
		RETURNING *
		""")
	Mono<Order> insertOrder(
			@Param("orderNo") String orderNo,
			@Param("customerId") Long customerId,
			@Param("storeId") UUID storeId,
			@Param("productId") UUID productId,
			@Param("orderType") String orderType,
			@Param("status") String status,
			@Param("cancelableUntil") LocalDateTime cancelableUntil,
			@Param("totalAmount") Integer totalAmount,
			@Param("idempotencyKey") String idempotencyKey,
			@Param("version") Long version
	);

	@Query("""
		UPDATE p_orders
		SET order_no = :orderNo,
			customer_id = :customerId,
			store_id = :storeId,
			product_id = :productId,
			order_type = CAST(:orderType AS order_type),
			status = CAST(:status AS order_status),
			cancelable_until = :cancelableUntil,
			total_amount = :totalAmount,
			idempotency_key = :idempotencyKey,
			version = :version,
			updated_at = NOW()
		WHERE id = :id
		RETURNING *
		""")
	Mono<Order> updateOrder(
			@Param("id") UUID id,
			@Param("orderNo") String orderNo,
			@Param("customerId") Long customerId,
			@Param("storeId") UUID storeId,
			@Param("productId") UUID productId,
			@Param("orderType") String orderType,
			@Param("status") String status,
			@Param("cancelableUntil") LocalDateTime cancelableUntil,
			@Param("totalAmount") Integer totalAmount,
			@Param("idempotencyKey") String idempotencyKey,
			@Param("version") Long version
	);
}
