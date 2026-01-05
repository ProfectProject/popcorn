package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.PaymentCommandService;
import com.popcorn.demo.global.config.CommonConfig;

class OrderAtddTest {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	private OrderCommandService orderCommandService;
	private PaymentCommandService paymentCommandService;

	@BeforeEach
	void setUp() {
		orderCommandService = Mockito.mock(OrderCommandService.class);
		paymentCommandService = Mockito.mock(PaymentCommandService.class);
		objectMapper = new CommonConfig().objectMapper();

		// ATDD 테스트 - 주문 생성과 상태 변경은 Command 작업이므로 OrderCommandController를 사용
		OrderCommandController commandController = new OrderCommandController(
				orderCommandService, objectMapper, paymentCommandService);
		mockMvc = MockMvcBuilders.standaloneSetup(commandController)
				.setControllerAdvice(new OrderExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("ATDD - 주문 생성 후 상태 변경 시나리오")
	void createOrder_then_updateStatus() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000002001");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000000020");

		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(orderId)
				.orderNo("O20260101-000001")
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

		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.OWNER_ACCEPTED)
				.build();
		when(orderCommandService.updateStatus(orderId, "OWNER_ACCEPTED", "approved"))
				.thenReturn(updatedOrder);

		String createJsonRequest = """
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
						.content(createJsonRequest))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(itemId.toString()));

		UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
				.status("OWNER_ACCEPTED")
				.reason("approved")
				.build();
		String statusBody = objectMapper.writeValueAsString(statusRequest);

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(statusBody))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("OWNER_ACCEPTED"));
	}
}
