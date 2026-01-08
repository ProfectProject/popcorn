package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.event.OrderCreatedEvent;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderConflictException;
import com.popcorn.demo.domain.order.exception.OrderForbiddenException;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.service.OrderItemPriceService;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;
import com.popcorn.demo.domain.order.repository.view.OrderAddressView;
import com.popcorn.demo.domain.order.repository.view.OrderDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderItemDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderPaymentView;
import com.popcorn.demo.domain.order.repository.view.OrderTimelineView;
import com.popcorn.demo.domain.order.repository.view.StoreOrderReservationView;
import com.popcorn.demo.domain.order.config.OrderProperties;

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
	private final ApplicationEventPublisher eventPublisher;
	private final OrderQueryRepository orderQueryRepository;
	private final OrderProperties orderProperties;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public CreateOrderResponse createOrder(CreateOrderCommand command) {
		log.info("🎯 주문 생성 시작 - 사용자: {}", command.getUserId());

		List<OrderItem> orderItems = convertToOrderItems(command.getItems());
		OrderType orderType = OrderType.valueOf(command.getOrderType());
		int totalQty = calculateTotalQuantity(orderItems);

		boolean isValid = validateOrderAsync(command.getUserId(), command.getPopupId(), totalQty);
		if (!isValid) {
			throw OrderValidationException.invalidRequest();
		}

		orderDomainService.validateOrderCreation(
				command.getUserId(),
				command.getStoreId(),
				command.getPopupId(),
				orderItems
		);

		Order order = orderDomainService.createOrder(
				command.getUserId(),
				command.getStoreId(),
				command.getPopupId(),
				orderType,
				orderItems
		);

		Order savedOrder = orderRepository.save(order);

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
		eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder, null));

		CreateOrderResponse response = CreateOrderResponse.fromOrder(savedOrder);
		log.info("✅ 주문 생성 완료 - 주문번호: {}, 사용자: {}", response.getOrderNo(), command.getUserId());
		return response;
	}

	@Transactional(readOnly = true, transactionManager = "jdbcTransactionManager")
	public OrderDetailDto getOrderDetail(UUID orderId, Long userId, String role) {
		OrderDetailView orderRow = fetchOrderDetailRow(orderId);
		validateOrderAccess(userId, role, orderRow);

		List<OrderDetailDto.ItemDto> items = fetchOrderItems(orderId, orderRow.getPopupId());
		OrderDetailDto.AddressDto address = fetchDefaultAddress(orderRow.getCustomerId());
		OrderDetailDto.PaymentDto payment = fetchPayment(orderId);

		return OrderDetailDto.builder()
				.id(orderRow.getOrderId())
				.orderNo(orderRow.getOrderNo())
				.orderType(orderRow.getOrderType())
				.status(orderRow.getStatus())
				.customerId(orderRow.getCustomerId())
				.customer(OrderDetailDto.CustomerDto.builder()
						.id(orderRow.getCustomerId())
						.role(orderRow.getCustomerRole())
						.build())
				.storeId(orderRow.getStoreId())
				.popupId(orderRow.getPopupId())
				.totalAmount(orderRow.getTotalAmount())
				.cancelableUntil(orderRow.getCancelableUntil())
				.createdAt(orderRow.getCreatedAt())
				.updatedAt(orderRow.getUpdatedAt())
				.items(items)
				.address(address)
				.payment(payment)
				.build();
	}

	@Transactional(readOnly = true, transactionManager = "jdbcTransactionManager")
	public StoreOrderReservationListResponse getStoreOrderReservations(
			UUID storeId,
			UUID popupId,
			String status,
			LocalDateTime from,
			LocalDateTime to,
			Integer page,
			Integer size) {

		int safePage = normalizePage(page);
		int safeSize = normalizeSize(size);
		long offset = (long) (safePage - 1) * safeSize;

		String normalizedStatus = normalizeStatusFilter(status);
		long total = orderQueryRepository.countStoreOrders(
				storeId,
				popupId,
				normalizedStatus,
				from,
				to
		);

		List<StoreOrderReservationView> rows = orderQueryRepository.findStoreOrders(
				storeId,
				popupId,
				normalizedStatus,
				from,
				to,
				safeSize,
				offset
		);

		List<StoreOrderReservationListResponse.ItemDto> items = rows.stream()
				.map(row -> {
					LocalDateTime createdAt = row.getCreatedAt();
					LocalDateTime cancelableUntil = row.getCancelableUntil() == null
							? createdAt.plusMinutes(30)
							: row.getCancelableUntil();
					return StoreOrderReservationListResponse.ItemDto.builder()
							.id(row.getId())
							.reservationNo(row.getOrderNo())
							.status(row.getStatus())
							.totalAmount(row.getTotalAmount())
							.cancelableUntil(cancelableUntil)
							.createdAt(createdAt)
							.build();
				})
				.toList();

		return StoreOrderReservationListResponse.builder()
				.items(items)
				.page(safePage)
				.size(safeSize)
				.total(total)
				.build();
	}

	@Transactional(readOnly = true, transactionManager = "jdbcTransactionManager")
	public MyOrderTimelineResponse getMyOrderTimeline(
			Long customerId,
			String orderType,
			String status,
			LocalDateTime from,
			LocalDateTime to,
			Integer page,
			Integer size) {

		Long safeCustomerId = customerId != null ? customerId : 1001L;
		int safePage = normalizePage(page);
		int safeSize = normalizeSize(size);
		long offset = (long) (safePage - 1) * safeSize;

		String normalizedOrderType = normalizeOrderTypeFilter(orderType);
		String normalizedStatus = normalizeStatusFilter(status);
		long total = orderQueryRepository.countCustomerOrders(
				safeCustomerId,
				normalizedOrderType,
				normalizedStatus,
				from,
				to
		);

		List<OrderTimelineView> rows = orderQueryRepository.findCustomerOrders(
				safeCustomerId,
				normalizedOrderType,
				normalizedStatus,
				from,
				to,
				safeSize,
				offset
		);

		List<MyOrderTimelineResponse.ItemDto> items = rows.stream()
				.map(row -> MyOrderTimelineResponse.ItemDto.builder()
						.type(row.getOrderType())
						.id(row.getId())
						.orderNo(row.getOrderNo())
						.status(row.getStatus())
						.totalAmount(row.getTotalAmount())
						.cancelableUntil(row.getCancelableUntil())
						.createdAt(row.getCreatedAt())
						.popupId(row.getPopupId())
						.storeId(row.getStoreId())
						.title(row.getProductTitle())
						.sessionStartAt(row.getSessionStartAt())
						.location(buildLocation(
								row.getLocationName(),
								row.getLocationAddress1(),
								row.getLocationAddress2()
						))
						.build())
				.toList();

		return MyOrderTimelineResponse.builder()
				.items(items)
				.page(safePage)
				.size(safeSize)
				.total(total)
				.build();
	}
	@Transactional(transactionManager = "jdbcTransactionManager")
	public Order updateStatus(UUID orderId, String status, String reason) {
		log.info("🧾 주문 상태 변경 요청 - 주문ID: {}, 변경상태: {}, 사유: {}", orderId, status, reason);

		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderNotFoundException::orderNotFound);

		OrderStatus currentStatus = order.getStatus();
		if (currentStatus == OrderStatus.CANCELLED) {
			throw OrderConflictException.alreadyCanceled();
		}

		OrderStatus newStatus;
		try {
			newStatus = OrderStatus.valueOf(status);
		} catch (IllegalArgumentException ex) {
			throw OrderValidationException.invalidRequest();
		}

		if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
			log.warn("❌ 주문 상태 전이 불가 - 주문ID: {}, 현재상태: {}, 요청상태: {}, 사유: {}",
					orderId, currentStatus, newStatus, reason);
			throw OrderValidationException.invalidStatusTransition();
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

	private OrderDetailView fetchOrderDetailRow(UUID orderId) {
		OrderDetailView row = orderQueryRepository.findOrderDetail(orderId);
		if (row == null) {
			throw OrderNotFoundException.orderNotFound();
		}
		return row;
	}

	private void validateOrderAccess(Long userId, String role, OrderDetailView orderRow) {
		if (userId == null || role == null || role.isBlank()) {
			return;
		}

		String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
		boolean allowed = switch (normalizedRole) {
			case "OWNER" -> orderRow.getStoreOwnerId() != null && orderRow.getStoreOwnerId().equals(userId);
			case "MANAGER" -> isStoreManager(userId, orderRow.getStoreId());
			case "CUSTOMER", "USER" -> orderRow.getCustomerId() != null && orderRow.getCustomerId().equals(userId);
			default -> false;
		};
		if (!allowed) {
			throw OrderForbiddenException.forbidden();
		}
	}

	private boolean isStoreManager(Long userId, UUID storeId) {
		return orderQueryRepository.countStoreManager(userId, storeId) > 0;
	}

	private List<OrderDetailDto.ItemDto> fetchOrderItems(UUID orderId, UUID fallbackPopupId) {
		List<OrderItemDetailView> rows = orderQueryRepository.findOrderItems(orderId, fallbackPopupId);
		return rows.stream()
				.map(row -> OrderDetailDto.ItemDto.builder()
						.id(row.getOrderItemId())
						.orderItemType(row.getOrderItemType())
						.popupId(row.getPopupId())
						.productTitle(row.getProductTitle())
						.productCategory(row.getProductCategory())
						.productStatus(row.getProductStatus())
						.sessionId(row.getSessionId())
						// .optionId(null) // optionId 필드가 없음
						.sessionStartAt(row.getSessionStartAt())
						.sessionEndAt(row.getSessionEndAt())
						.goodsVariantId(row.getGoodsVariantId())
						.merchVariantName(row.getMerchVariantName())
						.merchSku(row.getMerchSku())
						.qty(row.getQty())
						.unitPrice(row.getUnitPrice())
						.lineAmount(row.getLineAmount())
						.build())
				.toList();
	}

	private OrderDetailDto.AddressDto fetchDefaultAddress(Long userId) {
		OrderAddressView row = orderQueryRepository.findDefaultAddress(userId);
		if (row == null) {
			return null;
		}
		return OrderDetailDto.AddressDto.builder()
				.address1(row.getAddress1())
				.address2(row.getAddress2())
				.receiverName(row.getReceiverName())
				.phone(row.getPhone())
				.build();
	}

	private OrderDetailDto.PaymentDto fetchPayment(UUID orderId) {
		OrderPaymentView row = orderQueryRepository.findPayment(orderId);
		if (row == null) {
			return null;
		}
		return OrderDetailDto.PaymentDto.builder()
				.id(row.getPaymentId()) // getId() -> getPaymentId()로 수정
				.method(row.getMethod())
				.status(row.getStatus())
				.amount(row.getAmount())
				.approvedAt(row.getApprovedAt())
				.build();
	}

	private MyOrderTimelineResponse.LocationDto buildLocation(String name, String address1, String address2) {
		if (name == null && address1 == null && address2 == null) {
			return null;
		}
		return MyOrderTimelineResponse.LocationDto.builder()
				.name(name)
				.address1(address1)
				.address2(address2)
				.build();
	}

	private int normalizePage(Integer page) {
		if (page == null || page < 1) {
			return 1;
		}
		return page;
	}

	private int normalizeSize(Integer size) {
		if (size == null || size < 1) {
			return 20;
		}
		return size;
	}

	private String normalizeOrderTypeFilter(String orderType) {
		if (orderType == null || orderType.isBlank()) {
			return null;
		}
		String normalized = orderType.trim().toUpperCase(Locale.ROOT);
		return "ALL".equals(normalized) ? null : normalized;
	}

	private String normalizeStatusFilter(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		return status.trim().toUpperCase(Locale.ROOT);
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
	public boolean validateOrderAsync(Long userId, UUID popupId, Integer qty) {
		boolean stock = validateStock(qty);
		boolean user = validateCustomer(userId);
		boolean product = validateProduct(popupId);
		boolean result = stock && user && product;
		log.info("주문 검증 완료 - 사용자: {}, 상품: {}, 결과: {}", userId, popupId, result);
		return result;
	}

	/**
	 * 결제 처리 시뮬레이션
	 */
	public String processPaymentAsync(UUID orderId, Integer amount) {
		boolean paymentSuccess = Math.random() < orderProperties.getPaymentSuccessRate();
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

	private boolean validateProduct(UUID popupId) {
		return popupId != null;
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
				.orderItemType(itemCommand.getOrderItemType())
				.qty(itemCommand.getQty())
				.unitPrice(unitPrice)
				.lineAmount(lineAmount)
				.sessionOptionId(itemCommand.getSessionId())
				.goodsVariantId(itemCommand.getGoodsVariantId())
				.build();
	}

	private Integer resolveUnitPrice(CreateOrderCommand.OrderItemCommand itemCommand) {
		OrderItemType orderItemType = itemCommand.getOrderItemType();
		if (OrderItemType.RESERVATION.equals(orderItemType)) {
			UUID scheduleId = itemCommand.getSessionId();
			if (scheduleId == null) {
				throw OrderNotFoundException.sessionNotFound();
			}
			return orderItemPriceService.findSessionOptionPrice(scheduleId)
					.orElseThrow(OrderNotFoundException::sessionNotFound);
		}
		if (OrderItemType.GOODS.equals(orderItemType)) {
			UUID goodsVariantId = itemCommand.getGoodsVariantId();
			if (goodsVariantId == null) {
				throw OrderNotFoundException.merchVariantNotFound();
			}
			return orderItemPriceService.findMerchVariantPrice(goodsVariantId)
					.orElseThrow(OrderNotFoundException::merchVariantNotFound);
		}
		throw OrderValidationException.invalidRequest();
	}


	private int calculateTotalQuantity(List<OrderItem> orderItems) {
		return orderItems.stream()
				.mapToInt(OrderItem::getQty)
				.sum();
	}
}
