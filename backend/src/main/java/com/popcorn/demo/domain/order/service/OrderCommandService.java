package com.popcorn.demo.domain.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.event.OrderCreatedEvent;
import com.popcorn.demo.domain.order.event.OrderStatusChangedEvent;
import com.popcorn.demo.domain.order.event.OrderCancelledEvent;
import com.popcorn.demo.domain.order.event.OrderCompletedEvent;
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
	private final IdempotencyService idempotencyService;
	private final AsyncEventPublisher asyncEventPublisher;
	private final OrderValidationService orderValidationService;

	/**
	 * 주문 생성 (향상된 멱등성 처리)
	 */
	@Transactional(transactionManager = "jdbcTransactionManager")
	public CreateOrderResponse createOrder(CreateOrderCommand command) {
		log.info("🎯 주문 생성 시작 - 사용자: {}, 멱등성키: {}", command.getUserId(), command.getIdempotencyKey());

		// 향상된 멱등성 처리로 주문 생성
		IdempotencyService.IdempotencyResult<CreateOrderResponse> result =
			idempotencyService.processRequest(
				command.getIdempotencyKey(),
				() -> executeOrderCreation(command),
				CreateOrderResponse.class
			);

		if (result.isCached()) {
			log.info("✨ 멱등성 캐시에서 응답 반환 - 사용자: {}, 실행시간: {}",
					command.getUserId(), result.getExecutedAt());
		} else {
			log.info("🎉 새로운 주문 생성 완료 - 사용자: {}", command.getUserId());
		}

		return result.getResult();
	}

	/**
	 * 실제 주문 생성 로직 (멱등성 서비스가 호출)
	 */
	private CreateOrderResponse executeOrderCreation(CreateOrderCommand command) throws Exception {
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

		// 주문 아이템 저장
		List<OrderItem> itemsWithOrderId = savedOrder.getOrderItems().stream()
				.peek(item -> item.setOrderId(savedOrder.getId()))
				.toList();
		orderRepository.saveOrderItems(itemsWithOrderId);

		// 🚀 비동기 후처리 (향상된 이벤트 발행)
		asyncEventPublisher.publishEventAsync(new OrderCreatedEvent(savedOrder, command.getIdempotencyKey()))
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

		// 🚀 상태 변경 이벤트 발행
		asyncEventPublisher.publishEventAsync(new OrderStatusChangedEvent(
				savedOrder.getId(),
				savedOrder.getCustomerId(),
				currentStatus,
				newStatus,
				reason,
				"SYSTEM" // 변경 주체 - 실제로는 현재 사용자 정보를 사용
		)).whenComplete((result, throwable) -> {
			if (throwable == null) {
				log.debug("✅ 주문 상태 변경 이벤트 발행 완료 - 주문ID: {}, {} → {}",
						savedOrder.getId(), currentStatus, newStatus);
			} else {
				log.error("❌ 주문 상태 변경 이벤트 발행 실패 - 주문ID: {}", savedOrder.getId(), throwable);
			}
		});

		// 특별한 상태 변경의 경우 추가 이벤트 발행
		if (newStatus == OrderStatus.CANCELLED) {
			asyncEventPublisher.publishEventAsync(new OrderCancelledEvent(
					savedOrder.getId(),
					savedOrder.getCustomerId(),
					currentStatus,
					reason,
					"SYSTEM",
					null // 환불 금액은 별도 계산 필요
			));
		} else if (newStatus == OrderStatus.COMPLETED) {
			asyncEventPublisher.publishEventAsync(new OrderCompletedEvent(
					savedOrder.getId(),
					savedOrder.getCustomerId(),
					savedOrder.getStoreId(),
					savedOrder.getCreatedAt(),
					"SYSTEM",
					savedOrder.getTotalAmount(),
					savedOrder.getOrderItems().size()
			));
		}

		log.info("✅ 주문 상태 변경 완료 - 주문ID: {}, 이전상태: {}, 변경상태: {}",
				savedOrder.getId(), currentStatus, newStatus);

		return savedOrder;
	}

	// ================ 내부 헬퍼 메서드들 ================


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


	private int calculateTotalQuantity(List<OrderItem> orderItems) {
		return orderItems.stream()
				.mapToInt(OrderItem::getQty)
				.sum();
	}
}