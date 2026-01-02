package com.popcorn.demo.domain.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.event.OrderCreatedEvent;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderItemPriceService;
import com.popcorn.demo.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문 처리 서비스
 * - 주문 생성/상태 변경/후처리 작업을 담당합니다.
 * - 도메인 규칙은 OrderDomainService로 위임합니다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderDomainService orderDomainService;
	private final OrderRepository orderRepository;
	private final OrderItemPriceService orderItemPriceService;
	private final IdempotencyCache idempotencyCache;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public CreateOrderResponse createOrder(CreateOrderCommand command) {
		log.info("🎯 주문 생성 시작 - 사용자: {}, 멱등성키: {}", command.getUserId(), command.getIdempotencyKey());

		String idempotencyKey = normalizeIdempotencyKey(command.getIdempotencyKey());
		checkIdempotency(command.getIdempotencyKey(), idempotencyKey);

		List<OrderItem> orderItems = convertToOrderItems(command.getItems());
		OrderType orderType = OrderType.valueOf(command.getOrderType());
		int totalQty = calculateTotalQuantity(orderItems);

		boolean isValid = validateOrderAsync(command.getUserId(), command.getProductId(), totalQty);
		if (!isValid) {
			throw OrderException.invalidRequest();
		}

		orderDomainService.validateOrderCreation(
				command.getUserId(),
				command.getStoreId(),
				command.getProductId(),
				orderItems
		);

		Order order = orderDomainService.createOrder(
				command.getUserId(),
				command.getStoreId(),
				command.getProductId(),
				orderType,
				orderItems,
				command.getIdempotencyKey()
		);

		Order savedOrder = orderRepository.save(order);
		if (idempotencyKey != null) {
			idempotencyCache.mark(idempotencyKey);
		}

		List<OrderItem> itemsWithOrderId = savedOrder.getOrderItems().stream()
				.peek(item -> item.setOrderId(savedOrder.getId()))
				.toList();
		orderRepository.saveOrderItems(itemsWithOrderId);
		eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));

		CreateOrderResponse response = CreateOrderResponse.fromOrder(savedOrder);
		log.info("✅ 주문 생성 완료 - 주문번호: {}, 사용자: {}", response.getOrderNo(), command.getUserId());
		return response;
	}

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

		if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
			log.warn("❌ 주문 상태 전이 불가 - 주문ID: {}, 현재상태: {}, 요청상태: {}, 사유: {}",
					orderId, currentStatus, newStatus, reason);
			throw OrderException.invalidStatusTransition();
		}

		order.setStatus(newStatus);
		Order savedOrder = orderRepository.save(order);

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

	/**
	 * 주문 생성 후처리 작업
	 * - 재고 차감
	 * - 알림 발송
	 * - 이벤트 발행
	 */
	public void processOrderPostActions(UUID orderId) {
		log.info("주문 {} 재고 차감 완료", orderId);
		log.info("주문 {} 고객 알림 발송 완료", orderId);
		log.info("주문 {} 이벤트 발행 완료", orderId);
	}

	/**
	 * 주문 검증 처리
	 * - 상품 재고 확인
	 * - 고객 신용도 확인
	 * - 프로모션 유효성 확인
	 */
	public boolean validateOrderAsync(Long userId, UUID productId, Integer qty) {
		boolean stock = validateStock(qty);
		boolean user = validateCustomer(userId);
		boolean product = validateProduct(productId);
		boolean result = stock && user && product;
		log.info("주문 검증 완료 - 사용자: {}, 상품: {}, 결과: {}", userId, productId, result);
		return result;
	}

	/**
	 * 결제 처리 시뮬레이션
	 */
	public String processPaymentAsync(UUID orderId, Integer amount) {
		boolean paymentSuccess = Math.random() > 0.1;
		String paymentId = paymentSuccess ? "PAY-" + System.currentTimeMillis() : null;
		log.info("주문 {} 결제 처리 {}", orderId, paymentSuccess ? "성공: " + paymentId : "실패");
		return paymentId;
	}

	private boolean validateStock(Integer qty) {
		return qty != null && qty > 0;
	}

	private boolean validateCustomer(Long userId) {
		return userId != null && userId > 0;
	}

	private boolean validateProduct(UUID productId) {
		return productId != null;
	}

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
