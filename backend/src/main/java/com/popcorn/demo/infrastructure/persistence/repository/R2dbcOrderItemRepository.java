package com.popcorn.demo.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

import com.popcorn.demo.domain.order.entity.OrderItem;

public interface R2dbcOrderItemRepository extends ReactiveCrudRepository<OrderItem, UUID> {
	@Query("""
		INSERT INTO p_order_items (
			order_id,
			order_item_type,
			session_option_id,
			merch_variant_id,
			qty,
			unit_price,
			line_amount,
			created_at,
			updated_at
		) VALUES (
			:orderId,
			CAST(:orderItemType AS order_item_type),
			:sessionOptionId,
			:merchVariantId,
			:qty,
			:unitPrice,
			:lineAmount,
			NOW(),
			NOW()
		)
		RETURNING *
		""")
	Mono<OrderItem> insertOrderItem(
			@Param("orderId") UUID orderId,
			@Param("orderItemType") String orderItemType,
			@Param("sessionOptionId") UUID sessionOptionId,
			@Param("merchVariantId") UUID merchVariantId,
			@Param("qty") Integer qty,
			@Param("unitPrice") Integer unitPrice,
			@Param("lineAmount") Integer lineAmount
	);
}
