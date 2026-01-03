package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;

import com.popcorn.demo.global.config.CommonConfig;

class OrderControllerTest {

	private MockMvc mockMvc;

	private OrderCommandService orderCommandService;
	private OrderQueryService orderQueryService;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		orderCommandService = Mockito.mock(OrderCommandService.class);
		orderQueryService = Mockito.mock(OrderQueryService.class);
		objectMapper = new CommonConfig().objectMapper();

		// Command와 Query 작업을 모두 테스트하므로 두 컨트롤러 모두 설정
		OrderCommandController commandController = new OrderCommandController(orderCommandService, objectMapper);
		OrderQueryController queryController = new OrderQueryController(orderQueryService);
		mockMvc = MockMvcBuilders.standaloneSetup(commandController, queryController)
				.setControllerAdvice(new OrderExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("주문 생성 성공")
	void createOrder_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000000010");

		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(orderId)
				.orderNo("O20251231-000001")
				.orderType("RESERVATION")
				.status("REQUESTED")
				.storeId(storeId)
				.productId(productId)
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

		when(orderCommandService.createOrder(any())).thenReturn(response);

		String jsonRequest = """
				{
					"orderType": "RESERVATION",
					"storeId": "%s",
					"productId": "%s",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 2
						}
					]
				}
				""".formatted(storeId, productId);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("REQUESTED"));
	}

	@Test
	@DisplayName("주문 생성 실패 - 빈 아이템")
	void createOrder_fail_emptyItems() throws Exception {
		when(orderCommandService.createOrder(any())).thenThrow(OrderException.emptyItems());

		String jsonRequest = """
				{
					"orderType": "RESERVATION",
					"storeId": "00000000-0000-0000-0000-000000000001",
					"productId": "00000000-0000-0000-0000-000000000101",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 1
						}
					]
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
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
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.paymentStatus").value("READY"));
	}

	@Test
	@DisplayName("주문 생성 실패 - 잘못된 수량")
	void createOrder_fail_invalidQty() throws Exception {
		when(orderCommandService.createOrder(any())).thenThrow(OrderException.invalidQty());

		String jsonRequest = """
				{
					"orderType": "RESERVATION",
					"storeId": "00000000-0000-0000-0000-000000000001",
					"productId": "00000000-0000-0000-0000-000000000101",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 1
						}
					]
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1001))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("수량은 1 이상이어야 합니다."));
	}

	@Test
	@DisplayName("주문 생성 실패 - 상품 없음")
	void createOrder_fail_productNotFound() throws Exception {
		when(orderCommandService.createOrder(any())).thenThrow(OrderException.productNotFound());

		String jsonRequest = """
				{
					"orderType": "RESERVATION",
					"storeId": "00000000-0000-0000-0000-000000000001",
					"productId": "00000000-0000-0000-0000-000000000999",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 1
						}
					]
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1102))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("상품을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("주문 생성 실패 - 멱등성 키 중복")
	void createOrder_fail_duplicateIdempotency() throws Exception {
		when(orderCommandService.createOrder(any())).thenThrow(OrderException.duplicateIdempotencyKey());

		String jsonRequest = """
				{
					"orderType": "RESERVATION",
					"storeId": "00000000-0000-0000-0000-000000000001",
					"productId": "00000000-0000-0000-0000-000000000101",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 1
						}
					]
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", "test-key-dup")
						.content(jsonRequest))
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
				.status(OrderStatus.OWNER_ACCEPTED)
				.build();

		when(orderCommandService.updateStatus(orderId, "OWNER_ACCEPTED", "approved"))
				.thenReturn(updatedOrder);

		String jsonRequest = """
				{
					"status": "OWNER_ACCEPTED",
					"reason": "approved"
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("OWNER_ACCEPTED"));
	}

	@Test
	@DisplayName("주문 상태 변경 성공 - OWNER_ACCEPTED → CONFIRMED")
	void updateOrderStatus_ownerAcceptedToConfirmed() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001115");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.CONFIRMED)
				.build();

		when(orderCommandService.updateStatus(orderId, "CONFIRMED", "confirmed"))
				.thenReturn(updatedOrder);

		String jsonRequest = """
				{
					"status": "CONFIRMED",
					"reason": "confirmed"
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("CONFIRMED"));
	}

	@Test
	@DisplayName("주문 상태 변경 성공 - CONFIRMED → PREPARING")
	void updateOrderStatus_confirmedToPreparing() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001116");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.PREPARING)
				.build();

		when(orderCommandService.updateStatus(orderId, "PREPARING", "preparing"))
				.thenReturn(updatedOrder);

		String jsonRequest = """
				{
					"status": "PREPARING",
					"reason": "preparing"
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("PREPARING"));
	}

	@Test
	@DisplayName("주문 상태 변경 실패 - 허용되지 않은 전이")
	void updateOrderStatus_invalidTransition() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001112");
		when(orderCommandService.updateStatus(orderId, "READY", "reason"))
				.thenThrow(OrderException.invalidStatusTransition());

		String jsonRequest = """
				{
					"status": "READY",
					"reason": "reason"
				}
				""";

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
		when(orderCommandService.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
				.thenThrow(OrderException.orderNotFound());

		String jsonRequest = """
				{
					"status": "OWNER_ACCEPTED",
					"reason": "reason"
				}
				""";

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
		when(orderCommandService.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
				.thenThrow(OrderException.alreadyCanceled());

		String jsonRequest = """
				{
					"status": "OWNER_ACCEPTED",
					"reason": "reason"
				}
				""";

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
				.thenThrow(OrderException.invalidRequest());

		String jsonRequest = """
				{
					"status": "NOT_A_STATUS",
					"reason": "reason"
				}
				""";

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
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(UUID.randomUUID())
				.orderNo("O20251231-000001")
				.orderType("RESERVATION")
				.status("REQUESTED")
				.storeId(storeId)
				.productId(productId)
				.totalAmount(2000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.items(List.of())
				.build();

		when(orderCommandService.createOrder(any())).thenReturn(response);

		String jsonRequest = """
				{
					"orderType": "RESERVATION",
					"storeId": "%s",
					"productId": "%s",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 2
						}
					]
				}
				""".formatted(storeId, productId);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonRequest))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		verify(orderCommandService).createOrder(any());
	}

	@Test
	@DisplayName("가게 주문/예약 목록 조회 성공")
	void getStoreOrders_success() throws Exception {
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
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

		when(orderQueryService.getStoreOrderReservations(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/store")
						.param("storeId", storeId.toString()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(reservationId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].reservationNo").value("O20260102-000001"));
	}

	@Test
	@DisplayName("가게 주문/예약 목록 조회 실패 - 권한 없음")
	void getStoreOrders_forbidden() throws Exception {
		when(orderQueryService.getStoreOrderReservations(any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(OrderException.forbidden());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/store")
						.param("storeId", "00000000-0000-0000-0000-000000000001"))
				.andExpect(MockMvcResultMatchers.status().isForbidden())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."));
	}

	@Test
	@DisplayName("내 주문/예약 타임라인 조회 성공")
	void getMyOrders_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001301");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
								.productId(productId)
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

		when(orderQueryService.getMyOrderTimeline(null, "ALL", null, null, null, 20, 0L))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/me"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value("팝업 테스트"));
	}

	@Test
	@DisplayName("내 주문/예약 타임라인 조회 실패 - 잘못된 요청")
	void getMyOrders_invalidRequest() throws Exception {
		when(orderQueryService.getMyOrderTimeline(any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(OrderException.invalidRequest());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/me"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("잘못된 요청입니다."));
	}

}
