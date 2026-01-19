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
				OrderStatus.ACCEPTED,
				OrderStatus.PAYMENT_PENDING,
				OrderStatus.RESERVED,
				OrderStatus.REJECTED,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.ACCEPTED, java.util.EnumSet.of(
				OrderStatus.RESERVED,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.RESERVED, java.util.EnumSet.of(
				OrderStatus.PAYMENT_PENDING,
				OrderStatus.PAID,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.PAYMENT_PENDING, java.util.EnumSet.of(
				OrderStatus.PAID,
				OrderStatus.CANCELLED
		));
		transitions.put(OrderStatus.PAID, java.util.EnumSet.of(
				OrderStatus.COMPLETED,
				OrderStatus.CANCELLED  // 5분 이내 결제 취소 허용
		));
		transitions.put(OrderStatus.REJECTED, java.util.EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.CANCELLED, java.util.EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.COMPLETED, java.util.EnumSet.noneOf(OrderStatus.class));
		return transitions;
	}



	/**

		* 주문 중복 체크는 DB의 PK 제약을 활용합니다.

		*

		* 동일한 UUID로 주문 생성 시도 시 DB에서 자동으로 제약 위반 오류가 발생하고,

		* 이를 Service Layer에서 잡아서 "이미 처리된 주문입니다" 메시지로 변환합니다.

		*

		* 이 메서드는 더 이상 필요하지 않으며, DB 제약을 활용하는 것이 더 안전합니다.

		*/

	@Deprecated

	public boolean isDuplicateOrder() {

		// DB PK 제약을 활용하므로 이 메서드는 더 이상 사용하지 않음

		return false;

	}



	/**

		* 주문 생성 전 필수 검증을 수행합니다.

		*

		* @param customerId 고객 ID

		* @param storeId 스토어 ID

		* @param popupId 상품 ID

		* @param orderItems 주문 항목들

		* @throws com.popcorn.demo.global.exception.BaseException 검증 실패 시

		*/

	public void validateOrderCreation(Long customerId, UUID storeId, UUID popupId,

			List<OrderItem> orderItems) {

		if (customerId == null || customerId <= 0) {

			throw OrderValidationException.invalidRequest();

		}



		if (storeId == null) {

			throw OrderNotFoundException.storeNotFound();

		}



		if (popupId == null) {

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
		// 결제 완료 후 5분 이내에만 취소 가능하도록 설정합니다.
		return switch (orderType) {
			case RESERVATION -> now.plusMinutes(5); // 예약형: 5분 후까지 취소 가능
			case PURCHASE -> now.plusMinutes(5); // 구매형: 5분 후까지 취소 가능
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

		* @param popupId 상품 ID

		* @param orderType 주문 타입

		* @param orderItems 주문 항목들

		* @return 생성된 주문 엔티티

		*/

	public Order createOrder(Long customerId, UUID storeId, UUID popupId,

			OrderType orderType, List<OrderItem> orderItems) {



		// 1. 검증: 필수 값과 아이템 조건을 사전에 체크합니다.

		validateOrderCreation(customerId, storeId, popupId, orderItems);



		// 2. 비즈니스 룰 적용: 취소 가능 시간/총액 계산

		LocalDateTime cancelableUntil = calculateCancelableUntil(orderType);

		int totalAmount = calculateTotalAmount(orderItems);



		// 3. 주문 엔티티 생성: 기본 상태는 REQUESTED

		Order order = Order.builder()

				.orderNo(Order.generateOrderNo())

				.customerId(customerId)

				.storeId(storeId)

				.popupId(popupId)

				.orderType(orderType)

				.status(OrderStatus.REQUESTED)

				.totalAmount(totalAmount)

				.cancelableUntil(cancelableUntil)

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
