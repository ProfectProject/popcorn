package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.usecase.CreateOrderUseCase;
import com.popcorn.demo.application.order.usecase.UpdateOrderStatusUseCase;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;

import lombok.extern.slf4j.Slf4j;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = OrderController.class)
@Import({OrderExceptionHandler.class, OrderControllerTest.TestConfig.class})
@Slf4j
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CreateOrderUseCase createOrderUseCase;

	@Autowired
	private UpdateOrderStatusUseCase updateOrderStatusUseCase;

	@Test
	void createOrder_success() throws Exception {
		log.info("🧪 주문 생성 컨트롤러 테스트 시작");
		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(1L)
				.orderNo("O20251231-000001")
				.orderType("RESERVATION")
				.status("REQUESTED")
				.storeId(1L)
				.productId(1L)
				.totalAmount(2000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.items(List.of(
						CreateOrderResponse.OrderItemResponse.builder()
								.itemId(10L)
								.orderItemType(OrderItemType.RESERVATION.name())
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();

		when(createOrderUseCase.createOrder(any())).thenReturn(response);

		String body = """
				{
				  "orderType": "RESERVATION",
				  "storeId": 1,
				  "productId": 1,
				  "items": [
				    {
				      "orderItemType": "RESERVATION",
				      "sessionId": 1,
				      "optionId": 1,
				      "qty": 2
				    }
				  ]
				}
				""";

		mockMvc.perform(post("/api/v1/orders/1001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.status").value("REQUESTED"));
		log.info("✅ 주문 생성 컨트롤러 테스트 완료");
	}

	@Test
	void updateOrderStatus_success() throws Exception {
		log.info("🧪 주문 상태 변경 컨트롤러 테스트 시작");
		Order updatedOrder = Order.builder()
				.id(1L)
				.status(OrderStatus.OWNER_ACCEPTED)
				.build();

		when(updateOrderStatusUseCase.updateStatus(eq(1L), eq("OWNER_ACCEPTED"), eq("approved")))
				.thenReturn(updatedOrder);

		String body = """
				{
				  "status": "OWNER_ACCEPTED",
				  "reason": "approved"
				}
				""";

		mockMvc.perform(patch("/api/v1/orders/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.status").value("OWNER_ACCEPTED"));
		log.info("✅ 주문 상태 변경 컨트롤러 테스트 완료");
	}

	@Test
	void updateOrderStatus_invalidTransition() throws Exception {
		log.info("🧪 주문 상태 변경 실패 케이스 테스트 시작");
		when(updateOrderStatusUseCase.updateStatus(eq(1L), eq("READY"), eq("reason")))
				.thenThrow(OrderException.invalidStatusTransition());

		String body = """
				{
				  "status": "READY",
				  "reason": "reason"
				}
				""";

		mockMvc.perform(patch("/api/v1/orders/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400));
		log.info("✅ 주문 상태 변경 실패 케이스 테스트 완료");
	}

	@TestConfiguration
	static class TestConfig {
		@Bean
		CreateOrderUseCase createOrderUseCase() {
			return Mockito.mock(CreateOrderUseCase.class);
		}

		@Bean
		UpdateOrderStatusUseCase updateOrderStatusUseCase() {
			return Mockito.mock(UpdateOrderStatusUseCase.class);
		}
	}
}
