package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;

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
	private static final java.util.Map<OrderStatus, java.util.EnumSet<OrderStatus>> STATUS_TRANSITIONS =
			buildStatusTransitions();

	private static java.util.Map<OrderStatus, java.util.EnumSet<OrderStatus>> buildStatusTransitions() {
		java.util.Map<OrderStatus, java.util.EnumSet<OrderStatus>> transitions =
				new java.util.EnumMap<>(OrderStatus.class);
		transitions.put(OrderStatus.REQUESTED, java.util.EnumSet.of(
				OrderStatus.CONFIRMED,
				OrderStatus.OWNER_ACCEPTED,
				OrderStatus.OWNER_REJECTED,
				OrderStatus.COMPLETED,
				OrderStatus.PAID,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.OWNER_ACCEPTED, java.util.EnumSet.of(
				OrderStatus.PREPARING,
				OrderStatus.CONFIRMED,
				OrderStatus.READY,
				OrderStatus.COMPLETED,
				OrderStatus.PAID,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.CONFIRMED, java.util.EnumSet.of(
				OrderStatus.PREPARING,
				OrderStatus.COMPLETED,
				OrderStatus.PAID,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.PREPARING, java.util.EnumSet.of(
				OrderStatus.READY,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.READY, java.util.EnumSet.of(
				OrderStatus.COMPLETED,
				OrderStatus.PAID,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.PAID, java.util.EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.OWNER_REJECTED, java.util.EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.CANCELLED, java.util.EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.REFUNDED, java.util.EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.COMPLETED, java.util.EnumSet.noneOf(OrderStatus.class));
		return transitions;
	}



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

		* @throws com.popcorn.demo.global.exception.BaseException 검증 실패 시

		*/

	public void validateOrderCreation(Long customerId, UUID storeId, UUID productId,

			List<OrderItem> orderItems) {

		if (customerId == null || customerId <= 0) {

			throw OrderValidationException.invalidRequest();

		}



		if (storeId == null) {

			throw OrderNotFoundException.storeNotFound();

		}



		if (productId == null) {

			throw OrderNotFoundException.productNotFound();

		}



		if (orderItems == null || orderItems.isEmpty()) {

			throw OrderValidationException.emptyItems();

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

			throw OrderValidationException.invalidQty();

		}



		if (item.getUnitPrice() <= 0) {

			throw OrderValidationException.invalidRequest();

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

		// 주문 타입별 취소 가능 시간을 계산합니다.
		// 예약형은 비교적 여유를 주고, 구매형은 짧게 설정합니다.
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

		// 각 아이템의 (단가 * 수량)을 합산해 총액을 계산합니다.
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

	public Order createOrder(Long customerId, UUID storeId, UUID productId,

			OrderType orderType, List<OrderItem> orderItems,

			String idempotencyKey) {



		// 1. 검증: 필수 값과 아이템 조건을 사전에 체크합니다.

		validateOrderCreation(customerId, storeId, productId, orderItems);



		// 2. 비즈니스 룰 적용: 취소 가능 시간/총액 계산

		LocalDateTime cancelableUntil = calculateCancelableUntil(orderType);

		int totalAmount = calculateTotalAmount(orderItems);



		// 3. 주문 엔티티 생성: 기본 상태는 REQUESTED

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

				.build();

		// 4. 주문 항목과 주문 간의 연관관계 설정
		order.addOrderItems(new ArrayList<>(orderItems));



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

		// 현재 상태 기준으로 허용된 전이 목록에 포함되는지 확인합니다.
		java.util.EnumSet<OrderStatus> allowed = STATUS_TRANSITIONS.get(currentStatus);
		return allowed != null && allowed.contains(newStatus);

	}



	/**

		* 주문 취소 가능 여부 검증

		*

		* @param order 주문 엔티티

		* @return 취소 가능 여부

		*/

	public boolean canCancelOrder(Order order) {

		// 취소 가능 시간 + 상태 전이 가능 여부를 동시에 확인합니다.
		return order.isCancelable()

				&& canChangeStatus(order.getStatus(), OrderStatus.CANCELLED);

	}

}
