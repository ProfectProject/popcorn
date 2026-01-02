package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.usecase.CreateOrderUseCase;
import com.popcorn.demo.application.order.usecase.UpdateOrderStatusUseCase;
import com.popcorn.demo.common.config.CommonConfig;
import com.popcorn.demo.common.config.SecurityConfig;
import com.popcorn.demo.domain.order.dto.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.OrderItemRequest;
import com.popcorn.demo.domain.order.dto.UpdateOrderStatusRequest;
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
		CommonConfig.class,
		OrderControllerTest.TestConfig.class
})
@ActiveProfiles("local")
@Slf4j
class OrderControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CreateOrderUseCase createOrderUseCase;

	@Autowired
	private UpdateOrderStatusUseCase updateOrderStatusUseCase;

	@Test
	void createOrder_success() throws Exception {
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

		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.storeId(storeId)
				.productId(productId)
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.fromString("00000000-0000-0000-0000-000000000201"))
								.optionId(UUID.fromString("00000000-0000-0000-0000-000000000301"))
								.qty(2)
								.build()
				))
				.build();
		String body = objectMapper.writeValueAsString(request);

		webTestClient.post()
				.uri("/api/v1/orders/1001")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.code").isEqualTo(200)
				.jsonPath("$.data.orderId").isEqualTo(orderId.toString())
				.jsonPath("$.data.status").isEqualTo("REQUESTED")
				.jsonPath("$.data.items[0].id").isEqualTo(itemId.toString());
		log.info("✅ 주문 생성 컨트롤러 테스트 완료");
	}

	@Test
	void updateOrderStatus_success() throws Exception {
		log.info("🧪 주문 상태 변경 컨트롤러 테스트 시작");
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001111");
		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.OWNER_ACCEPTED)
				.build();

		when(updateOrderStatusUseCase.updateStatus(orderId, "OWNER_ACCEPTED", "approved"))
				.thenReturn(Mono.just(updatedOrder));

		UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
				.status("OWNER_ACCEPTED")
				.reason("approved")
				.build();
		String body = objectMapper.writeValueAsString(request);

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
	void updateOrderStatus_invalidTransition() throws Exception {
		log.info("🧪 주문 상태 변경 실패 케이스 테스트 시작");
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001112");
		when(updateOrderStatusUseCase.updateStatus(orderId, "READY", "reason"))
				.thenReturn(Mono.error(OrderException.invalidStatusTransition()));

		UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
				.status("READY")
				.reason("reason")
				.build();
		String body = objectMapper.writeValueAsString(request);

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

	@Test
	void createOrder_then_updateStatus_success() throws Exception {
		log.info("🧪 주문 생성 → 상태 변경 ATDD 시나리오 테스트 시작");
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

		when(createOrderUseCase.createOrder(any())).thenReturn(Mono.just(response));

		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.OWNER_ACCEPTED)
				.build();

		when(updateOrderStatusUseCase.updateStatus(orderId, "OWNER_ACCEPTED", "approved"))
				.thenReturn(Mono.just(updatedOrder));

		CreateOrderRequest createRequest = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.storeId(storeId)
				.productId(productId)
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.fromString("00000000-0000-0000-0000-000000000201"))
								.optionId(UUID.fromString("00000000-0000-0000-0000-000000000301"))
								.qty(2)
								.build()
				))
				.build();
		String createBody = objectMapper.writeValueAsString(createRequest);

		webTestClient.post()
				.uri("/api/v1/orders/1001")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(createBody)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.data.orderId").isEqualTo(orderId.toString())
				.jsonPath("$.data.items[0].id").isEqualTo(itemId.toString());

		UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
				.status("OWNER_ACCEPTED")
				.reason("approved")
				.build();
		String statusBody = objectMapper.writeValueAsString(statusRequest);

		webTestClient.patch()
				.uri("/api/v1/orders/" + orderId + "/status")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(statusBody)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.id").isEqualTo(orderId.toString())
				.jsonPath("$.data.status").isEqualTo("OWNER_ACCEPTED");
		log.info("✅ 주문 생성 → 상태 변경 ATDD 시나리오 테스트 완료");
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
