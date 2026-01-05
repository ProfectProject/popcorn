package com.popcorn.demo.domain.order.repository.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.order.entity.OrderItem;

public interface JpaOrderItemRepository extends JpaRepository<OrderItem, UUID> {
	boolean existsByOrderIdAndSessionOptionIdIsNotNull(UUID orderId);

	boolean existsByOrderIdAndGoodsVariantIdIsNotNull(UUID orderId);
}
