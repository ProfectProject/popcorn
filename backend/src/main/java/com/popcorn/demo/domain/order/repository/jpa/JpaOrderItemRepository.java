package com.popcorn.demo.domain.order.repository.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.order.entity.OrderItem;

public interface JpaOrderItemRepository extends JpaRepository<OrderItem, UUID> {
	boolean existsByOrderIdAndSessionOptionIdIsNotNull(UUID orderId);

	boolean existsByOrderIdAndGoodsVariantIdIsNotNull(UUID orderId);

	/**
	 * 특정 주문의 모든 주문 항목 조회
	 */
	List<OrderItem> findByOrderId(UUID orderId);
}
