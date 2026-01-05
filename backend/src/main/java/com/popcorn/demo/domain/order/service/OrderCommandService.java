package com.popcorn.demo.domain.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.event.OrderCreatedEvent;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문 명령(Command) 처리 서비스
 *
 * CQRS 패턴 적용으로 OrderService 분리:
 * - 주문 생성, 상태 변경 등 데이터 변경 작업만 담당
 * - 조회 작업은 OrderQueryService로 분리
 * - 성능 최적화와 책임 분리 달성
 */
@Service
@RequiredArgsConstructor
public class OrderCommandService {

	private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);

	private final OrderDomainService orderDomainService;
	private final OrderRepository orderRepository;
	private final OrderItemPriceService orderItemPriceService;
	private final IdempotencyCache idempotencyCache;
	private final AsyncEventPublisher asyncEventPublisher;
	private final OrderValidationService orderValidationService;

	/**
	 * 주문 생성 (최적화된 비동기 처리)
	 */
	@Transactional(transactionManager = "jdbcTransactionManager")
	public CreateOrderResponse createOrder(CreateOrderCommand command) {
		log.info("🎯 주문 생성 시작 - 사용자: {}, 멱등성키: {}", command.getUserId(), command.getIdempotencyKey());

		String idempotencyKey = normalizeIdempotencyKey(command.getIdempotencyKey());

		// 멱등성 검증
		checkIdempotency(command.getIdempotencyKey(), idempotencyKey);

		// 주문 항목 변환 및 검증
		List<OrderItem> orderItems = convertToOrderItems(command.getItems());
		OrderType orderType = OrderType.valueOf(command.getOrderType());

		// 비동기 검증 (성능 최적화)
		boolean isValid = orderValidationService.validateOrderAsync(
			command.getUserId(),
			command.getProductId(),
			calculateTotalQuantity(orderItems)
		);
		if (!isValid) {
			throw OrderException.invalidRequest();
		}

		// 도메인 검증
		orderDomainService.validateOrderCreation(
				command.getUserId(),
				command.getStoreId(),
				command.getProductId(),
				orderItems
		);

		// 주문 생성
		Order order = orderDomainService.createOrder(
				command.getUserId(),
				command.getStoreId(),
				command.getProductId(),
				orderType,
				orderItems,
				command.getIdempotencyKey()
		);

		// 동기 저장 (트랜잭션 내)
		Order savedOrder = orderRepository.save(order);

		// 멱등성 마킹
		if (idempotencyKey != null) {
			idempotencyCache.mark(idempotencyKey);
		}

		// 주문 아이템 저장
		List<OrderItem> itemsWithOrderId = savedOrder.getOrderItems().stream()
				.peek(item -> item.setOrderId(savedOrder.getId()))
				.toList();
		orderRepository.saveOrderItems(itemsWithOrderId);

		// 🚀 비동기 후처리 (최적화된 이벤트 발행)
		asyncEventPublisher.publishEventAsync(new OrderCreatedEvent(savedOrder))
				.whenComplete((result, throwable) -> {
					if (throwable == null) {
						log.debug("✅ 주문 생성 이벤트 발행 완료 - 주문ID: {}", savedOrder.getId());
					} else {
						log.error("❌ 주문 생성 이벤트 발행 실패 - 주문ID: {}", savedOrder.getId(), throwable);
					}
				});

		CreateOrderResponse response = CreateOrderResponse.fromOrder(savedOrder);
		log.info("✅ 주문 생성 완료 - 주문번호: {}, 사용자: {}", response.getOrderNo(), command.getUserId());

		return response;
	}

	/**
	 * 주문 상태 변경 (최적화된 처리)
	 */
	@Transactional(transactionManager = "jdbcTransactionManager")
	public Order updateStatus(UUID orderId, String status, String reason) {
		log.info("🧾 주문 상태 변경 요청 - 주문ID: {}, 변경상태: {}, 사유: {}", orderId, status, reason);

		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderException::orderNotFound);

		OrderStatus currentStatus = order.getStatus();
		if (currentStatus == OrderStatus.CANCELLED) {
			throw OrderException.alreadyCanceled();
		}

		OrderStatus newStatus;
		try {
			newStatus = OrderStatus.valueOf(status);
		} catch (IllegalArgumentException ex) {
			throw OrderException.invalidRequest();
		}

		// 도메인 검증
		if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
			log.warn("❌ 주문 상태 전이 불가 - 주문ID: {}, 현재상태: {}, 요청상태: {}, 사유: {}",
					orderId, currentStatus, newStatus, reason);
			throw OrderException.invalidStatusTransition();
		}

		// 상태 변경
		order.setStatus(newStatus);
		Order savedOrder = orderRepository.save(order);

		// 상태 이력 저장
		OrderStatusHistory statusHistory = OrderStatusHistory.builder()
				.orderId(savedOrder.getId())
				.fromStatus(currentStatus)
				.toStatus(savedOrder.getStatus())
				.reason(reason)
				.changedAt(java.time.LocalDateTime.now())
				.build();
		orderRepository.saveStatusHistory(statusHistory);

		log.info("✅ 주문 상태 변경 완료 - 주문ID: {}, 이전상태: {}, 변경상태: {}",
				savedOrder.getId(), currentStatus, newStatus);

		return savedOrder;
	}

	// ================ 내부 헬퍼 메서드들 ================

	private void checkIdempotency(String rawKey, String normalizedKey) {
		if (normalizedKey == null) {
			return;
		}
		if (idempotencyCache.isDuplicate(normalizedKey)) {
			log.warn("⚠️ 캐시 중복 주문 감지 - 멱등성키: {}", normalizedKey);
			throw OrderException.duplicateIdempotencyKey();
		}
		Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(rawKey);
		if (orderDomainService.isDuplicateOrder(existingOrder, rawKey)) {
			log.warn("⚠️ 중복 주문 요청 - 멱등성키: {}", rawKey);
			idempotencyCache.mark(normalizedKey);
			throw OrderException.duplicateIdempotencyKey();
		}
	}

	private List<OrderItem> convertToOrderItems(List<CreateOrderCommand.OrderItemCommand> itemCommands) {
		return itemCommands.stream()
				.map(this::convertToOrderItem)
				.toList();
	}

	private OrderItem convertToOrderItem(CreateOrderCommand.OrderItemCommand itemCommand) {
		Integer unitPrice = resolveUnitPrice(itemCommand);
		Integer lineAmount = unitPrice * itemCommand.getQty();
		return OrderItem.builder()
				.id(UUID.randomUUID())
				.orderItemType(itemCommand.getOrderItemType())
				.qty(itemCommand.getQty())
				.unitPrice(unitPrice)
				.lineAmount(lineAmount)
				.sessionOptionId(itemCommand.getOptionId())
				.merchVariantId(itemCommand.getMerchVariantId())
				.build();
	}

	private Integer resolveUnitPrice(CreateOrderCommand.OrderItemCommand itemCommand) {
		OrderItemType orderItemType = itemCommand.getOrderItemType();
		if (OrderItemType.RESERVATION.equals(orderItemType)) {
			UUID optionId = itemCommand.getOptionId();
			if (optionId == null) {
				throw OrderException.optionNotFound();
			}
			return orderItemPriceService.findSessionOptionPrice(optionId)
					.orElseThrow(OrderException::optionNotFound);
		}
		if (OrderItemType.MERCH.equals(orderItemType)) {
			UUID merchVariantId = itemCommand.getMerchVariantId();
			if (merchVariantId == null) {
				throw OrderException.merchVariantNotFound();
			}
			return orderItemPriceService.findMerchVariantPrice(merchVariantId)
					.orElseThrow(OrderException::merchVariantNotFound);
		}
		throw OrderException.invalidRequest();
	}

	private String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null) {
			return null;
		}
		String trimmed = idempotencyKey.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private int calculateTotalQuantity(List<OrderItem> orderItems) {
		return orderItems.stream()
				.mapToInt(OrderItem::getQty)
				.sum();
	}
}