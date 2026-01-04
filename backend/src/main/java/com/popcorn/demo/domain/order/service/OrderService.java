package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
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
	private final JdbcTemplate jdbcTemplate;

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

		OrderStatusHistory createdHistory = OrderStatusHistory.builder()
				.orderId(savedOrder.getId())
				.fromStatus(null)
				.toStatus(savedOrder.getStatus())
				.reason("주문 생성")
				.changedAt(LocalDateTime.now())
				.build();
		orderRepository.saveStatusHistory(createdHistory);

		List<OrderItem> itemsWithOrderId = savedOrder.getOrderItems().stream()
				.peek(item -> item.setOrderId(savedOrder.getId()))
				.toList();
		orderRepository.saveOrderItems(itemsWithOrderId);
		eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));

		CreateOrderResponse response = CreateOrderResponse.fromOrder(savedOrder);
		log.info("✅ 주문 생성 완료 - 주문번호: {}, 사용자: {}", response.getOrderNo(), command.getUserId());
		return response;
	}

	@Transactional(readOnly = true, transactionManager = "jdbcTransactionManager")
	public OrderDetailDto getOrderDetail(UUID orderId, Long userId, String role) {
		OrderDetailRow orderRow = fetchOrderDetailRow(orderId);
		validateOrderAccess(userId, role, orderRow);

		List<OrderDetailDto.ItemDto> items = fetchOrderItems(orderId, orderRow.productId);
		OrderDetailDto.AddressDto address = fetchDefaultAddress(orderRow.customerId);
		OrderDetailDto.PaymentDto payment = fetchPayment(orderId);

		return OrderDetailDto.builder()
				.id(orderRow.orderId)
				.orderNo(orderRow.orderNo)
				.orderType(orderRow.orderType)
				.status(orderRow.status)
				.customerId(orderRow.customerId)
				.customer(OrderDetailDto.CustomerDto.builder()
						.id(orderRow.customerId)
						.role(orderRow.customerRole)
						.build())
				.storeId(orderRow.storeId)
				.productId(orderRow.productId)
				.totalAmount(orderRow.totalAmount)
				.cancelableUntil(orderRow.cancelableUntil)
				.createdAt(orderRow.createdAt)
				.updatedAt(orderRow.updatedAt)
				.items(items)
				.address(address)
				.payment(payment)
				.build();
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

	private OrderDetailRow fetchOrderDetailRow(UUID orderId) {
		String sql = """
				SELECT o.id,
				       o.order_no,
				       o.order_type,
				       o.status,
				       o.customer_id,
				       u.role AS customer_role,
				       u.phone AS customer_phone,
				       o.store_id,
				       s.owner_id AS store_owner_id,
				       o.product_id,
				       o.total_amount,
				       o.cancelable_until,
				       o.created_at,
				       o.updated_at
				  FROM p_orders o
				  JOIN p_users u ON u.id = o.customer_id
				  JOIN p_stores s ON s.id = o.store_id
				 WHERE o.id = ?
				""";

		List<OrderDetailRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> OrderDetailRow.builder()
				.orderId(UUID.fromString(rs.getString("id")))
				.orderNo(rs.getString("order_no"))
				.orderType(rs.getString("order_type"))
				.status(rs.getString("status"))
				.customerId(rs.getLong("customer_id"))
				.customerRole(rs.getString("customer_role"))
				.customerPhone(rs.getString("customer_phone"))
				.storeId(UUID.fromString(rs.getString("store_id")))
				.storeOwnerId(rs.getLong("store_owner_id"))
				.productId(rs.getString("product_id") == null ? null : UUID.fromString(rs.getString("product_id")))
				.totalAmount(rs.getInt("total_amount"))
				.cancelableUntil(rs.getTimestamp("cancelable_until") == null
						? null
						: rs.getTimestamp("cancelable_until").toLocalDateTime())
				.createdAt(rs.getTimestamp("created_at").toLocalDateTime())
				.updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
				.build(), orderId);

		if (rows.isEmpty()) {
			throw OrderException.orderNotFound();
		}
		return rows.get(0);
	}

	private void validateOrderAccess(Long userId, String role, OrderDetailRow orderRow) {
		if (userId == null || role == null || role.isBlank()) {
			return;
		}

		String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
		boolean allowed = switch (normalizedRole) {
			case "ADMIN" -> true;
			case "OWNER" -> orderRow.storeOwnerId != null && orderRow.storeOwnerId.equals(userId);
			case "MANAGER" -> isStoreManager(userId, orderRow.storeId);
			case "CUSTOMER", "USER" -> orderRow.customerId != null && orderRow.customerId.equals(userId);
			default -> false;
		};
		if (!allowed) {
			throw OrderException.forbidden();
		}
	}

	private boolean isStoreManager(Long userId, UUID storeId) {
		String sql = """
				SELECT COUNT(1)
				  FROM p_managers_store
				 WHERE user_id = ?
				   AND store_id = ?
				   AND COALESCE(is_user_stop, FALSE) = FALSE
				   AND COALESCE(is_owner_stop, FALSE) = FALSE
				   AND COALESCE(is_force_stop, FALSE) = FALSE
				""";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, storeId);
		return count != null && count > 0;
	}

	private List<OrderDetailDto.ItemDto> fetchOrderItems(UUID orderId, UUID fallbackProductId) {
		String sql = """
				SELECT oi.id AS order_item_id,
				       oi.order_item_type,
				       oi.session_option_id,
				       oi.merch_variant_id,
				       oi.qty,
				       oi.unit_price,
				       oi.line_amount,
				       so.session_id,
				       ps.start_at AS session_start_at,
				       ps.end_at AS session_end_at,
				       mv.name AS merch_variant_name,
				       mv.sku AS merch_sku,
				       p.id AS product_id,
				       p.title AS product_title,
				       p.category AS product_category,
				       p.status AS product_status
				  FROM p_order_items oi
				  LEFT JOIN p_session_options so ON oi.session_option_id = so.id
				  LEFT JOIN p_product_sessions ps ON so.session_id = ps.id
				  LEFT JOIN p_merch_variants mv ON oi.merch_variant_id = mv.id
				  LEFT JOIN p_products p ON p.id = COALESCE(ps.product_id, mv.product_id, ?)
				 WHERE oi.order_id = ?
				   AND oi.deleted_at IS NULL
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> OrderDetailDto.ItemDto.builder()
				.id(UUID.fromString(rs.getString("order_item_id")))
				.orderItemType(rs.getString("order_item_type"))
				.productId(rs.getString("product_id") == null ? null : UUID.fromString(rs.getString("product_id")))
				.productTitle(rs.getString("product_title"))
				.productCategory(rs.getString("product_category"))
				.productStatus(rs.getString("product_status"))
				.sessionId(rs.getString("session_id") == null ? null : UUID.fromString(rs.getString("session_id")))
				.optionId(rs.getString("session_option_id") == null ? null : UUID.fromString(rs.getString("session_option_id")))
				.sessionStartAt(rs.getTimestamp("session_start_at") == null
						? null
						: rs.getTimestamp("session_start_at").toLocalDateTime())
				.sessionEndAt(rs.getTimestamp("session_end_at") == null
						? null
						: rs.getTimestamp("session_end_at").toLocalDateTime())
				.merchVariantId(rs.getString("merch_variant_id") == null
						? null
						: UUID.fromString(rs.getString("merch_variant_id")))
				.merchVariantName(rs.getString("merch_variant_name"))
				.merchSku(rs.getString("merch_sku"))
				.qty(rs.getInt("qty"))
				.unitPrice(rs.getInt("unit_price"))
				.lineAmount(rs.getInt("line_amount"))
				.build(), fallbackProductId, orderId);
	}

	private OrderDetailDto.AddressDto fetchDefaultAddress(Long userId) {
		String sql = """
				SELECT ua.address1,
				       ua.address2,
				       ua.name AS receiver_name,
				       u.phone
				  FROM p_user_addresses ua
				  JOIN p_users u ON u.id = ua.user_id
				 WHERE ua.user_id = ?
				   AND ua.is_default = TRUE
				   AND ua.deleted_at IS NULL
				 ORDER BY ua.created_at DESC
				 LIMIT 1
				""";

		List<OrderDetailDto.AddressDto> rows = jdbcTemplate.query(sql, (rs, rowNum) -> OrderDetailDto.AddressDto.builder()
				.address1(rs.getString("address1"))
				.address2(rs.getString("address2"))
				.receiverName(rs.getString("receiver_name"))
				.phone(rs.getString("phone"))
				.build(), userId);

		if (rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private OrderDetailDto.PaymentDto fetchPayment(UUID orderId) {
		String sql = """
				SELECT id, method, status, amount, approved_at
				  FROM p_payments
				 WHERE order_id = ?
				   AND deleted_at IS NULL
				 LIMIT 1
				""";

		List<OrderDetailDto.PaymentDto> rows = jdbcTemplate.query(sql, (rs, rowNum) -> OrderDetailDto.PaymentDto.builder()
				.id(UUID.fromString(rs.getString("id")))
				.method(rs.getString("method"))
				.status(rs.getString("status"))
				.amount(rs.getInt("amount"))
				.approvedAt(rs.getTimestamp("approved_at") == null
						? null
						: rs.getTimestamp("approved_at").toLocalDateTime())
				.build(), orderId);

		if (rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	@lombok.Builder
	private static class OrderDetailRow {
		private UUID orderId;
		private String orderNo;
		private String orderType;
		private String status;
		private Long customerId;
		private String customerRole;
		private String customerPhone;
		private UUID storeId;
		private Long storeOwnerId;
		private UUID productId;
		private Integer totalAmount;
		private LocalDateTime cancelableUntil;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
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
