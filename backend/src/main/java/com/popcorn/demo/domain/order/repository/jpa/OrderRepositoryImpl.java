package com.popcorn.demo.domain.order.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

/**
	* 도메인 OrderRepository 인터페이스의 JPA 구현체
	* - Clean Architecture: 어댑터 패턴 적용
	* - JPA Repository를 래핑하여 도메인 요구사항 충족
	*/
@Repository("orderRepositoryImpl")
public class OrderRepositoryImpl implements OrderRepository {

	private final JpaOrderItemRepository orderItemRepository;
	private final JpaOrderRepository orderRepository;
	private final JpaOrderStatusHistoryRepository orderStatusHistoryRepository;
	public OrderRepositoryImpl(
			JpaOrderItemRepository orderItemRepository,
			JpaOrderRepository orderRepository,
			JpaOrderStatusHistoryRepository orderStatusHistoryRepository) {
		this.orderItemRepository = orderItemRepository;
		this.orderRepository = orderRepository;
		this.orderStatusHistoryRepository = orderStatusHistoryRepository;
	}

	// ========================= 기본 CRUD 메서드 =========================

	@Override
	public Order save(Order order) {
		if (order == null) {
			return null;
		}
		Order savedOrder = orderRepository.save(order);
		savedOrder.setOrderItems(order.getOrderItems());
		return savedOrder;
	}

	@Override
	public void saveOrderItems(List<OrderItem> orderItems) {
		if (orderItems == null || orderItems.isEmpty()) {
			return;
		}
		orderItemRepository.saveAll(orderItems);
	}

	@Override
	public java.util.Optional<Order> findById(UUID orderId) {
		// 단건 상세 조회는 기본 ID 조회로 처리합니다.
		return orderRepository.findById(orderId);
	}

	@Override
	public java.util.Optional<OrderSummaryView> findSummaryById(UUID orderId) {
		// 상세 화면이 아닌 요약 화면에서 사용하는 조회입니다.
		return java.util.Optional.ofNullable(orderRepository.findSummaryById(orderId));
	}

	@Override
	public java.util.Optional<Order> findByOrderNo(String orderNo) {
		return orderRepository.findByOrderNo(orderNo);
	}

	@Override
	public void deleteById(UUID orderId) {
		orderRepository.deleteById(orderId);
	}

	@Override
	public void deleteAllOrders() {
		// 외래키 관계 때문에 순서대로 삭제
		orderStatusHistoryRepository.deleteAll();
		orderItemRepository.deleteAll();
		orderRepository.deleteAll();
	}

	@Override
	public boolean existsById(UUID orderId) {
		return orderRepository.existsById(orderId);
	}

	// ========================= 비즈니스 조회 메서드 =========================

	@Override
	public List<Order> findByCustomerId(Long customerId) {
		// 고객 기준 주문 목록을 최신순으로 조회합니다.
		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
	}

	@Override
	public List<Order> findByCustomerId(Long customerId, int offset, int limit) {
		// 페이징 파라미터는 음수 방지를 위해 보정합니다.
		long safeOffset = Math.max(0, offset);
		int safeLimit = Math.max(1, limit);
		return orderRepository.findByCustomerIdWithPaging(customerId, safeOffset, safeLimit);
	}

	@Override
	public List<OrderSummaryView> findSummariesByCustomerId(Long customerId, int offset, int limit) {
		// 목록 화면 전용으로 필요한 컬럼만 조회합니다.
		long safeOffset = Math.max(0, offset);
		int safeLimit = Math.max(1, limit);
		return orderRepository.findSummariesByCustomerIdWithPaging(customerId, safeOffset, safeLimit);
	}

	@Override
	public List<Order> findByStoreId(UUID storeId) {
		// 스토어 기준 주문 목록을 조회합니다.
		return orderRepository.findByStoreId(storeId);
	}

	@Override
	public List<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status) {
		// 스토어 + 상태 필터로 주문 목록을 조회합니다.
		return orderRepository.findByStoreIdAndStatus(storeId, status);
	}

	@Override
	public List<Order> findByStatus(OrderStatus status) {
		return orderRepository.findByStatus(status);
	}

	@Override
	public List<Order> findCancelableOrders(LocalDateTime currentTime) {
		return orderRepository.findCancelableOrders(currentTime);
	}

	@Override
	public List<Order> findExpiredCancelableOrders(LocalDateTime currentTime) {
		return orderRepository.findExpiredCancelableOrders(currentTime);
	}

	@Override
	public long countByCustomerId(Long customerId) {
		return orderRepository.countByCustomerId(customerId);
	}

	@Override
	public long countByStoreId(UUID storeId) {
		return orderRepository.countByStoreId(storeId);
	}

	@Override
	public long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
		return orderRepository.countByCreatedAtBetween(startDate, endDate);
	}

	@Override
	public long sumTotalAmountByCustomerIdAndCreatedAtBetween(
			Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
		return orderRepository.sumTotalAmountByCustomerIdAndCreatedAtBetween(customerId, startDate, endDate);
	}

	@Override
	public void saveStatusHistory(OrderStatusHistory history) {
		orderStatusHistoryRepository.save(history);
	}
}
