package com.popcorn.demo.infrastructure.persistence.repository;

import java.util.UUID;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

import com.popcorn.demo.domain.order.entity.OrderStatusHistory;

public interface R2dbcOrderStatusHistoryRepository extends ReactiveCrudRepository<OrderStatusHistory, UUID> {
	@Query("""
		INSERT INTO p_order_status_histories (
			order_id,
			from_status,
			to_status,
			reason,
			changed_at,
			created_at
		) VALUES (
			:orderId,
			CAST(:fromStatus AS order_status),
			CAST(:toStatus AS order_status),
			:reason,
			:changedAt,
			NOW()
		)
		RETURNING *
		""")
	Mono<OrderStatusHistory> insertStatusHistory(
			@Param("orderId") UUID orderId,
			@Param("fromStatus") String fromStatus,
			@Param("toStatus") String toStatus,
			@Param("reason") String reason,
			@Param("changedAt") LocalDateTime changedAt
	);
}
