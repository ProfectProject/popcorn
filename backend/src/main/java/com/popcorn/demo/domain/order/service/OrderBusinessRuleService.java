package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;

import lombok.RequiredArgsConstructor;

/**
 * 주문 비즈니스 규칙 검증 서비스
 *
 * 복잡한 비즈니스 규칙과 도메인 로직을 검증:
 * - 주문 생성 규칙 검증
 * - 주문 아이템 구성 검증
 * - 시간/날짜 관련 규칙 검증
 * - 금액/수량 제한 검증
 */
@Service
@RequiredArgsConstructor
public class OrderBusinessRuleService {

	private static final Logger log = LoggerFactory.getLogger(OrderBusinessRuleService.class);

	/**
	 * 주문 생성 비즈니스 규칙 검증
	 */
	public void validateOrderCreationRules(CreateOrderCommand command) {
		validateOrderTiming(command);
		validateOrderItems(command.getItems());
		validateOrderAmount(command.getItems());
		validateOrderItemCombination(command.getOrderType(), command.getItems());

		log.info("✅ 주문 생성 비즈니스 규칙 검증 완료 - 사용자: {}", command.getUserId());
	}

	/**
	 * 주문 상태 변경 비즈니스 규칙 검증
	 */
	public void validateStatusTransitionRules(OrderStatus currentStatus, OrderStatus targetStatus, String reason) {
		if (!isValidStatusTransition(currentStatus, targetStatus)) {
			log.warn("❌ 잘못된 상태 전이 - 현재: {}, 대상: {}", currentStatus, targetStatus);
			throw OrderException.invalidStatusTransition();
		}

		validateStatusTransitionTiming(currentStatus, targetStatus);
		validateStatusTransitionReason(targetStatus, reason);

		log.debug("✅ 상태 전이 규칙 검증 완료 - {} → {}", currentStatus, targetStatus);
	}

	/**
	 * 주문 취소 비즈니스 규칙 검증
	 */
	public void validateOrderCancellationRules(OrderStatus currentStatus, LocalDateTime orderCreatedAt) {
		// 취소 불가 상태 검증
		if (currentStatus == OrderStatus.COMPLETED ||
			currentStatus == OrderStatus.CANCELLED ||
			currentStatus == OrderStatus.REFUNDED) {
			log.warn("❌ 취소 불가 상태: {}", currentStatus);
			throw OrderException.orderCannotBeCancelled();
		}

		// 취소 시간 제한 검증 (예: 주문 후 30분 이내)
		LocalDateTime cancellationDeadline = orderCreatedAt.plusMinutes(30);
		if (LocalDateTime.now().isAfter(cancellationDeadline)) {
			log.warn("❌ 취소 시간 초과 - 주문시간: {}, 마감시간: {}", orderCreatedAt, cancellationDeadline);
			throw OrderException.cancellationTimeExpired();
		}

		log.debug("✅ 주문 취소 규칙 검증 완료");
	}

	// ================ 내부 검증 메서드들 ================

	private void validateOrderTiming(CreateOrderCommand command) {
		LocalDateTime now = LocalDateTime.now();

		// 영업시간 검증 (예: 오전 9시 ~ 오후 10시)
		int hour = now.getHour();
		if (hour < 9 || hour >= 22) {
			log.warn("❌ 영업시간 외 주문 - 현재시간: {}시", hour);
			throw OrderException.orderOutsideBusinessHours();
		}

		// 주문 마감시간 검증 (예: 세션 시작 30분 전까지)
		// TODO: 실제 세션 시간과 연동하여 검증
	}

	private void validateOrderItems(List<CreateOrderCommand.OrderItemCommand> items) {
		if (items == null || items.isEmpty()) {
			log.warn("❌ 빈 주문 아이템");
			throw OrderException.emptyItems();
		}

		if (items.size() > 10) { // 최대 10개 아이템 제한
			log.warn("❌ 주문 아이템 개수 초과: {}", items.size());
			throw OrderException.tooManyItems();
		}

		for (CreateOrderCommand.OrderItemCommand item : items) {
			validateIndividualItem(item);
		}
	}

	private void validateIndividualItem(CreateOrderCommand.OrderItemCommand item) {
		// 수량 검증
		if (item.getQty() == null || item.getQty() <= 0) {
			log.warn("❌ 잘못된 수량: {}", item.getQty());
			throw OrderException.invalidQty();
		}

		if (item.getQty() > 100) { // 최대 수량 제한
			log.warn("❌ 수량 제한 초과: {}", item.getQty());
			throw OrderException.quantityLimitExceeded();
		}

		// 아이템 타입별 필수 필드 검증
		switch (item.getOrderItemType()) {
			case RESERVATION -> {
				if (item.getSessionId() == null) {
					throw OrderException.sessionNotFound();
				}
				if (item.getOptionId() == null) {
					throw OrderException.optionNotFound();
				}
			}
			case MERCH -> {
				if (item.getMerchVariantId() == null) {
					throw OrderException.merchVariantNotFound();
				}
			}
		}
	}

	private void validateOrderAmount(List<CreateOrderCommand.OrderItemCommand> items) {
		int totalAmount = items.stream()
				.mapToInt(item -> (item.getUnitPrice() != null ? item.getUnitPrice() : 0) * item.getQty())
				.sum();

		// 최소 주문 금액 (1000원)
		if (totalAmount < 1000) {
			log.warn("❌ 최소 주문 금액 미달: {}", totalAmount);
			throw OrderException.belowMinimumOrderAmount();
		}

		// 최대 주문 금액 (100만원)
		if (totalAmount > 1_000_000) {
			log.warn("❌ 최대 주문 금액 초과: {}", totalAmount);
			throw OrderException.aboveMaximumOrderAmount();
		}
	}

	private void validateOrderItemCombination(String orderType, List<CreateOrderCommand.OrderItemCommand> items) {
		OrderType type = OrderType.valueOf(orderType);

		switch (type) {
			case RESERVATION -> {
				// 예약형 주문은 예약 아이템만 허용
				boolean hasNonReservationItem = items.stream()
						.anyMatch(item -> item.getOrderItemType() != OrderItemType.RESERVATION);
				if (hasNonReservationItem) {
					log.warn("❌ 예약형 주문에 비예약 아이템 포함");
					throw OrderException.invalidOrderItemCombination();
				}
			}
			case PURCHASE -> {
				// 구매형 주문은 굿즈 아이템만 허용
				boolean hasNonMerchItem = items.stream()
						.anyMatch(item -> item.getOrderItemType() != OrderItemType.MERCH);
				if (hasNonMerchItem) {
					log.warn("❌ 구매형 주문에 비굿즈 아이템 포함");
					throw OrderException.invalidOrderItemCombination();
				}
			}
		}
	}

	private boolean isValidStatusTransition(OrderStatus from, OrderStatus to) {
		// 상태 전이 규칙 매트릭스
		return switch (from) {
			case REQUESTED -> to == OrderStatus.OWNER_ACCEPTED ||
							  to == OrderStatus.OWNER_REJECTED ||
							  to == OrderStatus.CANCELLED;

			case OWNER_ACCEPTED -> to == OrderStatus.CONFIRMED ||
								   to == OrderStatus.CANCELLED;

			case CONFIRMED -> to == OrderStatus.PREPARING ||
							  to == OrderStatus.CANCELLED;

			case PREPARING -> to == OrderStatus.READY ||
							  to == OrderStatus.CANCELLED;

			case READY -> to == OrderStatus.COMPLETED ||
						  to == OrderStatus.CANCELLED;

			case OWNER_REJECTED, CANCELLED, COMPLETED, REFUNDED -> false; // 최종 상태들

			default -> false;
		};
	}

	private void validateStatusTransitionTiming(OrderStatus currentStatus, OrderStatus targetStatus) {
		// 특정 상태 전이에 대한 시간 제약 검증
		// 예: READY -> COMPLETED는 픽업 시간 이후에만 가능
		// TODO: 실제 비즈니스 요구사항에 따라 구현
	}

	private void validateStatusTransitionReason(OrderStatus targetStatus, String reason) {
		// 특정 상태 변경 시 사유 필수
		if ((targetStatus == OrderStatus.OWNER_REJECTED ||
			 targetStatus == OrderStatus.CANCELLED) &&
			(reason == null || reason.trim().isEmpty())) {
			log.warn("❌ 거절/취소 상태 변경 시 사유 필수");
			throw OrderException.reasonRequiredForRejectionOrCancellation();
		}
	}
}