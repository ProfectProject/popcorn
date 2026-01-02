package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.global.config.SecurityConfig;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;

import lombok.extern.slf4j.Slf4j;

@WebMvcTest(controllers = OrderController.class)
@Import({OrderExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("local")
@Slf4j
@SuppressWarnings("removal")
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderService orderService;

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

		when(orderService.createOrder(any())).thenReturn(response);

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
		when(orderService.createOrder(any())).thenThrow(OrderException.emptyItems());

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
	@DisplayName("주문 생성 실패 - 잘못된 수량")
	void createOrder_fail_invalidQty() throws Exception {
		when(orderService.createOrder(any())).thenThrow(OrderException.invalidQty());

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
		when(orderService.createOrder(any())).thenThrow(OrderException.productNotFound());

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
		when(orderService.createOrder(any())).thenThrow(OrderException.duplicateIdempotencyKey());

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

		when(orderService.updateStatus(orderId, "OWNER_ACCEPTED", "approved"))
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

		when(orderService.updateStatus(orderId, "CONFIRMED", "confirmed"))
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

		when(orderService.updateStatus(orderId, "PREPARING", "preparing"))
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
		when(orderService.updateStatus(orderId, "READY", "reason"))
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
		when(orderService.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
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
		when(orderService.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
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
		when(orderService.updateStatus(orderId, "NOT_A_STATUS", "reason"))
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

		when(orderService.createOrder(any())).thenReturn(response);

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

		verify(orderService).createOrder(any());
	}

}
