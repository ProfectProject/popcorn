package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import reactor.core.publisher.Mono;

import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.usecase.CreateOrderUseCase;
import com.popcorn.demo.application.order.usecase.UpdateOrderStatusUseCase;
import com.popcorn.demo.common.config.SecurityConfig;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;

import lombok.extern.slf4j.Slf4j;

@WebFluxTest(controllers = OrderController.class)
@ContextConfiguration(classes = {
		OrderController.class,
		OrderExceptionHandler.class,
		SecurityConfig.class,
		OrderControllerTest.TestConfig.class
})
@ActiveProfiles("local")
@Slf4j
class OrderControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private CreateOrderUseCase createOrderUseCase;

	@Autowired
	private UpdateOrderStatusUseCase updateOrderStatusUseCase;

	@Test
	void createOrder_success() {
		log.info("🧪 주문 생성 컨트롤러 테스트 시작");
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

		when(createOrderUseCase.createOrder(any())).thenReturn(Mono.just(response));

		String body = """
				{
				  "orderType": "RESERVATION",
				  "storeId": "00000000-0000-0000-0000-000000000001",
				  "productId": "00000000-0000-0000-0000-000000000101",
				  "items": [
				    {
				      "orderItemType": "RESERVATION",
				      "sessionId": "00000000-0000-0000-0000-000000000201",
				      "optionId": "00000000-0000-0000-0000-000000000301",
				      "qty": 2
				    }
				  ]
				}
				""";

		webTestClient.post()
				.uri("/api/v1/orders/1001")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.code").isEqualTo(200)
				.jsonPath("$.data.id").isEqualTo(orderId.toString())
				.jsonPath("$.data.status").isEqualTo("REQUESTED");
		log.info("✅ 주문 생성 컨트롤러 테스트 완료");
	}

	@Test
	void updateOrderStatus_success() {
		log.info("🧪 주문 상태 변경 컨트롤러 테스트 시작");
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001111");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.OWNER_ACCEPTED)
				.build();

		when(updateOrderStatusUseCase.updateStatus(eq(orderId), eq("OWNER_ACCEPTED"), eq("approved")))
				.thenReturn(Mono.just(updatedOrder));

		String body = """
				{
				  "status": "OWNER_ACCEPTED",
				  "reason": "approved"
				}
				""";

		webTestClient.patch()
				.uri("/api/v1/orders/" + orderId + "/status")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.code").isEqualTo(200)
				.jsonPath("$.data.id").isEqualTo(orderId.toString())
				.jsonPath("$.data.status").isEqualTo("OWNER_ACCEPTED");
		log.info("✅ 주문 상태 변경 컨트롤러 테스트 완료");
	}

	@Test
	void updateOrderStatus_invalidTransition() {
		log.info("🧪 주문 상태 변경 실패 케이스 테스트 시작");
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001112");
		when(updateOrderStatusUseCase.updateStatus(eq(orderId), eq("READY"), eq("reason")))
				.thenReturn(Mono.error(OrderException.invalidStatusTransition()));

		String body = """
				{
				  "status": "READY",
				  "reason": "reason"
				}
				""";

		webTestClient.patch()
				.uri("/api/v1/orders/" + orderId + "/status")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.code").isEqualTo(400);
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
