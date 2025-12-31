package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;

/**
 * 주문 도메인 서비스
 *
 * 순수한 비즈니스 로직만 포함합니다.
 * - 주문 생성 규칙
 * - 주문 검증 로직
 * - 비즈니스 불변식 검증
 *
 * 인프라스트럭처에 의존하지 않는 순수 도메인 로직입니다.
 */
@Service
public class OrderDomainService {

	/**
	 * 멱등성 키를 기반으로 중복 주문 여부를 판단합니다.
	 *
	 * @param existingOrder 기존 주문 (Optional)
	 * @param idempotencyKey 멱등성 키
	 * @return 중복 주문 여부
	 */
	public boolean isDuplicateOrder(Optional<Order> existingOrder, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
			return false;
		}
		return existingOrder.isPresent();
	}

	/**
	 * 주문 생성 전 필수 검증을 수행합니다.
	 *
	 * @param customerId 고객 ID
	 * @param storeId 스토어 ID
	 * @param productId 상품 ID
	 * @param orderItems 주문 항목들
	 * @throws OrderException 검증 실패 시
	 */
	public void validateOrderCreation(Long customerId, Long storeId, Long productId,
			List<OrderItem> orderItems) {
		if (customerId == null || customerId <= 0) {
			throw OrderException.invalidRequest();
		}

		if (storeId == null || storeId <= 0) {
			throw OrderException.storeNotFound();
		}

		if (productId == null || productId <= 0) {
			throw OrderException.productNotFound();
		}

		if (orderItems == null || orderItems.isEmpty()) {
			throw OrderException.emptyItems();
		}

		// 각 주문 항목별 검증
		for (OrderItem item : orderItems) {
			validateOrderItem(item);
		}
	}

	/**
	 * 개별 주문 항목 검증
	 */
	private void validateOrderItem(OrderItem item) {
		if (item.getQty() <= 0) {
			throw OrderException.invalidQty();
		}

		if (item.getUnitPrice() <= 0) {
			throw OrderException.invalidRequest();
		}
	}

	/**
	 * 주문 타입에 따른 비즈니스 룰 적용
	 *
	 * @param orderType 주문 타입
	 * @return 취소 가능 시간
	 */
	public LocalDateTime calculateCancelableUntil(OrderType orderType) {
		LocalDateTime now = LocalDateTime.now();
		return switch (orderType) {
			case RESERVATION -> now.plusDays(1); // 예약형: 1일 후까지 취소 가능
			case PURCHASE -> now.plusHours(1); // 구매형: 1시간 후까지 취소 가능
		};
	}

	/**
	 * 주문 총액 계산 (비즈니스 로직)
	 *
	 * @param orderItems 주문 항목들
	 * @return 총 주문 금액
	 */
	public int calculateTotalAmount(List<OrderItem> orderItems) {
		return orderItems.stream()
				.mapToInt(item -> item.getUnitPrice() * item.getQty())
				.sum();
	}

	/**
	 * 주문 엔티티를 생성합니다 (팩토리 메서드)
	 *
	 * @param customerId 고객 ID
	 * @param storeId 스토어 ID
	 * @param productId 상품 ID
	 * @param orderType 주문 타입
	 * @param orderItems 주문 항목들
	 * @param idempotencyKey 멱등성 키
	 * @return 생성된 주문 엔티티
	 */
	public Order createOrder(Long customerId, Long storeId, Long productId,
			OrderType orderType, List<OrderItem> orderItems,
			String idempotencyKey) {

		// 1. 검증
		validateOrderCreation(customerId, storeId, productId, orderItems);

		// 2. 비즈니스 룰 적용
		LocalDateTime cancelableUntil = calculateCancelableUntil(orderType);
		int totalAmount = calculateTotalAmount(orderItems);

		// 3. 주문 엔티티 생성
		Order order = Order.builder()
				.orderNo(Order.generateOrderNo())
				.customerId(customerId)
				.storeId(storeId)
				.productId(productId)
				.orderType(orderType)
				.status(OrderStatus.REQUESTED)
				.totalAmount(totalAmount)
				.cancelableUntil(cancelableUntil)
				.idempotencyKey(idempotencyKey)
				.orderItems(new ArrayList<>(orderItems))
				.build();

		// 4. 주문 항목과 주문 간의 연관관계 설정
		orderItems.forEach(item -> item.setOrder(order));

		return order;
	}

	/**
	 * 주문 상태 변경 가능 여부 검증
	 *
	 * @param currentStatus 현재 상태
	 * @param newStatus 변경하려는 상태
	 * @return 변경 가능 여부
	 */
	public boolean canChangeStatus(OrderStatus currentStatus, OrderStatus newStatus) {
		return switch (currentStatus) {
			case REQUESTED -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
			case CONFIRMED -> newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
			case PREPARING -> newStatus == OrderStatus.READY || newStatus == OrderStatus.CANCELLED;
			case READY -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;
			case CANCELLED, REFUNDED, COMPLETED -> false; // 최종 상태에서는 변경 불가
		};
	}

	/**
	 * 주문 취소 가능 여부 검증
	 *
	 * @param order 주문 엔티티
	 * @return 취소 가능 여부
	 */
	public boolean canCancelOrder(Order order) {
		return order.isCancelable()
				&& canChangeStatus(order.getStatus(), OrderStatus.CANCELLED);
	}
}
