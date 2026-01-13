package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderConflictException;
import com.popcorn.demo.domain.order.exception.OrderForbiddenException;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import com.popcorn.demo.domain.order.service.OrderPaymentFacade;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;

class OrderControllerTest extends OrderControllerTestBase {

	private static final String DEFAULT_STORE_ID = "00000000-0000-0000-0000-000000000001";
	private static final String DEFAULT_PRODUCT_ID = "00000000-0000-0000-0000-000000000101";
	private static final String DEFAULT_SESSION_ID = "00000000-0000-0000-0000-000000000201";
	private static final String DEFAULT_OPTION_ID = "00000000-0000-0000-0000-000000000301";

	@Test
	@DisplayName("주문 생성 성공")
	void createOrder_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);
		UUID productId = UUID.fromString(DEFAULT_PRODUCT_ID);
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000000010");

		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(orderId)
				.orderNo("O20251231-000001")
				.orderType("RESERVATION")
				.status("PAYMENT_PENDING")
				.storeId(storeId)
				.popupId(productId)
				.totalAmount(2000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.items(List.of(
						CreateOrderResponse.OrderItemResponse.builder()
								.itemId(itemId)
								.orderItemType(OrderItemType.RESERVATION.name())
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();

		when(orderPaymentFacade.createOrderWithPayment(any(), any()))
				.thenReturn(OrderPaymentFacade.OrderWithPaymentResult.builder()
						.orderResponse(response)
						.paymentResult(PaymentCommandService.PaymentCreationResult.builder()
								.paymentId(UUID.randomUUID())
								.amount(response.getTotalAmount())
								.customerId(1001L)
								.build())
						.build());

		String jsonRequest = buildReservationOrderRequest(productId.toString(), 2);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("PAYMENT_PENDING"));
	}

	@Test
	@DisplayName("주문 생성 실패 - 빈 아이템")
	void createOrder_fail_emptyItems() throws Exception {
		when(orderPaymentFacade.createOrderWithPayment(any(), any())).thenThrow(OrderValidationException.emptyItems());

		String jsonRequest = buildReservationOrderRequest(DEFAULT_PRODUCT_ID, 1);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("주문 항목이 비어있습니다."));
	}

	@Test
	@DisplayName("주문 상태 조회 성공 (CUSTOMER)")
	void getOrderStatus_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		OrderStatusDto response = OrderStatusDto.builder()
				.orderId(orderId)
				.orderNo("O20251231-001001")
				.status("REQUESTED")
				.paymentStatus("READY")
				.cancelableUntil(LocalDateTime.now().plusMinutes(15))
				.updatedAt(LocalDateTime.now())
				.build();

		when(orderQueryService.getOrderStatusForCustomer(orderId, 1001L)).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/status", orderId)
						.param("customerId", "1001")
						.contentType(MediaType.APPLICATION_JSON)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.paymentStatus").value("READY"));
	}

	@Test
	@DisplayName("주문 상태 조회 성공 (OWNER/MANAGER)")
	void getOrderStatusForStaff_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		OrderStatusDto response = OrderStatusDto.builder()
				.orderId(orderId)
				.orderNo("O20251231-001001")
				.status("REQUESTED")
				.paymentStatus("READY")
				.cancelableUntil(LocalDateTime.now().plusMinutes(15))
				.updatedAt(LocalDateTime.now())
				.build();

		// Use OWNER authentication that matches the expected userId and role
		Authentication ownerAuth = createTestAuthentication(2001L, "owner@example.com", UserRole.OWNER);
		when(orderQueryService.getOrderStatusForStaff(orderId, 2001L, "OWNER")).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/status/ops", orderId)
						.principal(ownerAuth)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.paymentStatus").value("READY"));
	}

	@Test
	@DisplayName("주문 상태 조회 실패 (OWNER/MANAGER) - 권한 없음")
	void getOrderStatusForStaff_forbidden() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");

		Authentication ownerAuth = createTestAuthentication(2001L, "owner@example.com", UserRole.OWNER);
		when(orderQueryService.getOrderStatusForStaff(orderId, 2001L, "OWNER"))
				.thenThrow(OrderForbiddenException.forbidden());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/status/ops", orderId)
						.principal(ownerAuth)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isForbidden())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."));
	}

	@Test
	@DisplayName("주문 상태 조회 실패 (OWNER/MANAGER) - 주문 없음")
	void getOrderStatusForStaff_notFound() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000009999");

		Authentication managerAuth = createTestAuthentication(2001L, "manager@example.com", UserRole.MANAGER);
		when(orderQueryService.getOrderStatusForStaff(orderId, 2001L, "MANAGER"))
				.thenThrow(OrderNotFoundException.orderNotFound());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/status/ops", orderId)
						.principal(managerAuth)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1100))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("주문을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("주문 상태 조회 성공 - CUSTOMER 권한으로 staff 엔드포인트 접근")
	void getOrderStatusForStaff_missingUserId() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");

		// Mock service call with CUSTOMER authentication (userId=1001L, role=CUSTOMER)
		when(orderQueryService.getOrderStatusForStaff(orderId, 1001L, "CUSTOMER"))
				.thenReturn(OrderStatusDto.builder()
						.orderId(orderId)
						.status("REQUESTED")
						.paymentStatus("READY")
						.build());

		// Test with CUSTOMER authentication - controller accepts any valid authentication
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/status/ops", orderId)
						.principal(createCustomerAuthentication())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200));
	}

	@Test
	@DisplayName("주문 상태 조회 실패 - 인증 없이 접근하면 NullPointerException")
	void getOrderStatusForStaff_missingRole() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");

		// Test without authentication - causes internal server error when trying to extract user info
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/status/ops", orderId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError());
	}

	@Test
	@DisplayName("주문 생성 실패 - 잘못된 수량")
	void createOrder_fail_invalidQty() throws Exception {
		when(orderPaymentFacade.createOrderWithPayment(any(), any())).thenThrow(OrderValidationException.invalidQty());

		String jsonRequest = buildReservationOrderRequest(DEFAULT_PRODUCT_ID, 1);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1001))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("수량은 1 이상이어야 합니다."));
	}

	@Test
	@DisplayName("주문 생성 실패 - 상품 없음")
	void createOrder_fail_productNotFound() throws Exception {
		when(orderPaymentFacade.createOrderWithPayment(any(), any())).thenThrow(OrderNotFoundException.productNotFound());

		String jsonRequest = buildReservationOrderRequest(
				"00000000-0000-0000-0000-000000000999",
				1
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1102))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("상품을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("주문 생성 실패 - 멱등성 키 중복")
	void createOrder_fail_duplicateIdempotency() throws Exception {
		when(orderPaymentFacade.createOrderWithPayment(any(), any())).thenThrow(OrderConflictException.duplicateIdempotencyKey());

		String jsonRequest = buildReservationOrderRequest(DEFAULT_PRODUCT_ID, 1);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", "test-key-dup")
						.content(jsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isConflict())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1302))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("중복된 요청입니다."));
	}

	@Test
	@DisplayName("주문 상태 변경 성공")
	void updateOrderStatus_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001111");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.ACCEPTED)
				.build();

		when(orderCommandService.updateStatus(orderId, "ACCEPTED", "approved"))
				.thenReturn(updatedOrder);

		String jsonRequest = buildUpdateStatusRequest("ACCEPTED", "approved");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACCEPTED"));
	}

	@Test
	@DisplayName("주문 상태 변경 성공 - ACCEPTED → RESERVED")
	void updateOrderStatus_ownerAcceptedToConfirmed() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001115");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.RESERVED)
				.build();

		when(orderCommandService.updateStatus(orderId, "RESERVED", "confirmed"))
				.thenReturn(updatedOrder);

		String jsonRequest = buildUpdateStatusRequest("RESERVED", "confirmed");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("RESERVED"));
	}

	@Test
	@DisplayName("주문 상태 변경 성공 - RESERVED → PAYMENT_PENDING")
	void updateOrderStatus_confirmedToPreparing() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001116");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.PAYMENT_PENDING)
				.build();

		when(orderCommandService.updateStatus(orderId, "PAYMENT_PENDING", "preparing"))
				.thenReturn(updatedOrder);

		String jsonRequest = buildUpdateStatusRequest("PAYMENT_PENDING", "preparing");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("PAYMENT_PENDING"));
	}

	@Test
	@DisplayName("주문 상태 변경 실패 - 허용되지 않은 전이")
	void updateOrderStatus_invalidTransition() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001112");
		when(orderCommandService.updateStatus(orderId, "PAYMENT_PENDING", "reason"))
				.thenThrow(OrderValidationException.invalidStatusTransition());

		String jsonRequest = buildUpdateStatusRequest("PAYMENT_PENDING", "reason");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1201))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("허용되지 않은 상태 변경입니다."));
	}

	@Test
	@DisplayName("주문 상태 변경 실패 - 주문 없음")
	void updateOrderStatus_notFound() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000009999");
		when(orderCommandService.updateStatus(orderId, "ACCEPTED", "reason"))
				.thenThrow(OrderNotFoundException.orderNotFound());

		String jsonRequest = buildUpdateStatusRequest("ACCEPTED", "reason");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1100))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("주문을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("주문 상태 변경 실패 - 이미 취소됨")
	void updateOrderStatus_alreadyCanceled() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001113");
		when(orderCommandService.updateStatus(orderId, "ACCEPTED", "reason"))
				.thenThrow(OrderConflictException.alreadyCanceled());

		String jsonRequest = buildUpdateStatusRequest("ACCEPTED", "reason");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isConflict())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1303))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 취소된 주문입니다."));
	}

	@Test
	@DisplayName("주문 상태 변경 실패 - 상태값 오류")
	void updateOrderStatus_invalidStatus() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001114");
		when(orderCommandService.updateStatus(orderId, "NOT_A_STATUS", "reason"))
				.thenThrow(OrderValidationException.invalidRequest());

		String jsonRequest = buildUpdateStatusRequest("NOT_A_STATUS", "reason");

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("잘못된 요청입니다."));
	}

	@Test
	@DisplayName("주문 생성 요청 시 서비스 호출")
	void createOrder_callsService() throws Exception {
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);
		UUID productId = UUID.fromString(DEFAULT_PRODUCT_ID);
		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(UUID.randomUUID())
				.orderNo("O20251231-000001")
				.orderType("RESERVATION")
				.status("PAYMENT_PENDING")
				.storeId(storeId)
				.popupId(productId)
				.totalAmount(2000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.items(List.of())
				.build();

		when(orderPaymentFacade.createOrderWithPayment(any(), any()))
				.thenReturn(OrderPaymentFacade.OrderWithPaymentResult.builder()
						.orderResponse(response)
						.paymentResult(PaymentCommandService.PaymentCreationResult.builder()
								.paymentId(UUID.randomUUID())
								.amount(response.getTotalAmount())
								.customerId(1001L)
								.build())
						.build());

		String jsonRequest = buildReservationOrderRequest(productId.toString(), 2);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		verify(orderPaymentFacade).createOrderWithPayment(any(), any());
	}

	@Test
	@DisplayName("가게 주문/예약 목록 조회 성공")
	void getStoreOrders_success() throws Exception {
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);
		UUID reservationId = UUID.fromString("00000000-0000-0000-0000-000000001201");
		LocalDateTime createdAt = LocalDateTime.now().minusHours(1);

		StoreOrderReservationListResponse response = StoreOrderReservationListResponse.builder()
				.items(List.of(
						StoreOrderReservationListResponse.ItemDto.builder()
								.id(reservationId)
								.reservationNo("O20260102-000001")
								.status("REQUESTED")
								.totalAmount(5000)
								.cancelableUntil(createdAt.plusMinutes(30))
								.createdAt(createdAt)
								.build()
				))
				.page(1)
				.size(20)
				.total(1)
				.build();

		when(orderQueryService.getStoreOrderReservations(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/store")
						.param("storeId", storeId.toString()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(reservationId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].reservationNo").value("O20260102-000001"));
	}

	@Test
	@DisplayName("가게 주문/예약 상태 목록 조회 성공 (OWNER/MANAGER)")
	void getStoreOrderStatusesForStaff_success() throws Exception {
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);
		UUID productId = UUID.fromString(DEFAULT_PRODUCT_ID);
		UUID reservationId = UUID.fromString("00000000-0000-0000-0000-000000001201");
		LocalDateTime createdAt = LocalDateTime.now().minusHours(1);

		StoreOrderReservationListResponse response = StoreOrderReservationListResponse.builder()
				.items(List.of(
						StoreOrderReservationListResponse.ItemDto.builder()
								.id(reservationId)
								.reservationNo("O20260102-000001")
								.status("REQUESTED")
								.totalAmount(5000)
								.cancelableUntil(createdAt.plusMinutes(30))
								.createdAt(createdAt)
								.build()
				))
				.page(1)
				.size(20)
				.total(1)
				.build();

		when(orderQueryService.getStoreOrderReservations(
				storeId, productId, null, null, "REQUESTED", null, null, 20, 0L
		)).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/status/ops")
						.param("storeId", storeId.toString())
						.param("popupId", productId.toString())
						.param("status", "REQUESTED")
						.param("page", "1")
						.param("size", "20"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(reservationId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].reservationNo").value("O20260102-000001"));
	}

	@Test
	@DisplayName("가게 주문/예약 상태 목록 조회 실패 (OWNER/MANAGER) - 권한 없음")
	void getStoreOrderStatusesForStaff_forbidden() throws Exception {
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);
		UUID productId = UUID.fromString(DEFAULT_PRODUCT_ID);

		when(orderQueryService.getStoreOrderReservations(
				storeId, productId, null, null, null, null, null, 20, 0L
		)).thenThrow(OrderForbiddenException.forbidden());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/status/ops")
						.param("storeId", storeId.toString())
						.param("popupId", productId.toString())
						.param("page", "1")
						.param("size", "20"))
				.andExpect(MockMvcResultMatchers.status().isForbidden())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."));
	}

	@Test
	@DisplayName("가게 주문/예약 상태 목록 조회 실패 (OWNER/MANAGER) - storeId/productId 누락")
	void getStoreOrderStatusesForStaff_missingFilter() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/status/ops")
						.param("page", "1")
						.param("size", "20"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("잘못된 요청입니다."));
	}

	@Test
	@DisplayName("가게 주문/예약 상태 목록 조회 실패 (OWNER/MANAGER) - status 파라미터 오류")
	void getStoreOrderStatusesForStaff_invalidStatus() throws Exception {
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);
		UUID productId = UUID.fromString(DEFAULT_PRODUCT_ID);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/status/ops")
						.param("storeId", storeId.toString())
						.param("popupId", productId.toString())
						.param("status", "NOT_A_STATUS")
						.param("page", "1")
						.param("size", "20"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("가게 주문/예약 목록 조회 실패 - 권한 없음")
	void getStoreOrders_forbidden() throws Exception {
		when(orderQueryService.getStoreOrderReservations(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(OrderForbiddenException.forbidden());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/store")
						.param("storeId", DEFAULT_STORE_ID))
				.andExpect(MockMvcResultMatchers.status().isForbidden())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."));
	}

	@Test
	@DisplayName("내 주문/예약 타임라인 조회 성공")
	void getMyOrders_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001301");
		UUID productId = UUID.fromString(DEFAULT_PRODUCT_ID);
		UUID storeId = UUID.fromString(DEFAULT_STORE_ID);

		MyOrderTimelineResponse response = MyOrderTimelineResponse.builder()
				.items(List.of(
						MyOrderTimelineResponse.ItemDto.builder()
								.type("RESERVATION")
								.id(orderId)
								.orderNo("O20260102-000101")
								.status("REQUESTED")
								.totalAmount(2000)
								.cancelableUntil(LocalDateTime.now().plusMinutes(30))
								.createdAt(LocalDateTime.now())
								.popupId(productId)
								.storeId(storeId)
								.title("팝업 테스트")
								.sessionStartAt(LocalDateTime.now().plusDays(1))
								.location(MyOrderTimelineResponse.LocationDto.builder()
										.name("팝업 장소")
										.address1("서울특별시 강남구 테헤란로 123")
										.address2("ABC빌딩 12층")
										.build())
								.build()
				))
				.page(1)
				.size(20)
				.total(1)
				.build();

		when(orderQueryService.getMyOrderTimeline(1001L, null, null, null, null, 20, 0L))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/me")
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value("팝업 테스트"));
	}

	@Test
	@DisplayName("내 주문/예약 타임라인 조회 실패 - 잘못된 요청")
	void getMyOrders_invalidRequest() throws Exception {
		when(orderQueryService.getMyOrderTimeline(any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(OrderValidationException.invalidRequest());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/me")
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("잘못된 요청입니다."));
	}

	@Test
	@DisplayName("모든 주문 데이터 삭제 성공")
	void deleteAllOrders_success() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/orders/all")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value("모든 주문 데이터가 삭제되었습니다."));

		verify(orderCommandService).deleteAllOrders();
	}

	private String buildReservationOrderRequest(String popupId, int qty) {
		return """
				{
					"orderType": "RESERVATION",
					"popupId": "%s",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "%s",
							"optionId": "%s",
							"qty": %d
						}
					]
				}
				""".formatted(popupId, DEFAULT_SESSION_ID, DEFAULT_OPTION_ID, qty);
	}

	private String buildUpdateStatusRequest(String status, String reason) {
		return """
				{
					"status": "%s",
					"reason": "%s"
				}
				""".formatted(status, reason);
	}
}
