package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.global.config.CommonConfig;

/**
 * 주문 취소 API 테스트 클래스 (TDD)
 *
 * 테스트 시나리오:
 * 1. 성공 케이스: 정상적인 주문 취소
 * 2. 실패 케이스: 존재하지 않는 주문 ID
 * 3. 실패 케이스: 이미 취소된 주문
 * 4. 실패 케이스: 취소할 수 없는 상태의 주문 (완료된 주문)
 * 5. 실패 케이스: 서버 내부 오류
 */
@DisplayName("주문 취소 API 테스트")
class OrderCancelControllerTest {

	private MockMvc mockMvc;
	private OrderCommandService orderCommandService;
	private ObjectMapper objectMapper;

	private UUID testOrderId;
	private Order mockOrder;

	@BeforeEach
	void setUp() {
		orderCommandService = Mockito.mock(OrderCommandService.class);
		objectMapper = new CommonConfig().objectMapper();

		// 주문 취소는 Command 작업이므로 OrderCommandController를 사용
		OrderCommandController commandController = new OrderCommandController(orderCommandService, objectMapper);
		mockMvc = MockMvcBuilders.standaloneSetup(commandController)
				.setControllerAdvice(new OrderExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();

		testOrderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		mockOrder = createMockOrder(testOrderId, OrderStatus.REQUESTED);
	}

	@Test
	@DisplayName("성공: 정상적인 주문 취소")
	void cancelOrder_Success() throws Exception {
		// Given: REQUESTED 상태의 주문이 있고, 취소 요청이 들어왔을 때
		Order cancelledOrder = createMockOrder(testOrderId, OrderStatus.CANCELLED);

		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenReturn(cancelledOrder);

		// When: 주문 취소 API 호출
		// Then: 200 응답과 함께 취소된 주문 정보 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
				.andExpect(jsonPath("$.data.id").value(testOrderId.toString()))
				.andExpect(jsonPath("$.data.status").value("CANCELED"));
	}

	@Test
	@DisplayName("실패: 존재하지 않는 주문 ID")
	void cancelOrder_NotFound() throws Exception {
		// Given: 존재하지 않는 주문 ID로 취소 요청
		UUID nonExistentOrderId = UUID.fromString("99999999-9999-9999-9999-999999999999");

		when(orderCommandService.updateStatus(eq(nonExistentOrderId), eq("CANCELLED"), any(String.class)))
				.thenThrow(OrderException.orderNotFound());

		// When & Then: 404 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", nonExistentOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 이미 취소된 주문")
	void cancelOrder_AlreadyCancelled() throws Exception {
		// Given: 이미 CANCELLED 상태인 주문
		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenThrow(OrderException.alreadyCanceled());

		// When & Then: 409 응답 반환 (비즈니스 규칙 위반)
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 취소할 수 없는 상태의 주문 (완료된 주문)")
	void cancelOrder_CannotCancel_CompletedOrder() throws Exception {
		// Given: COMPLETED 상태인 주문에 대한 취소 요청
		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenThrow(OrderException.invalidStatusTransition());

		// When & Then: 400 응답 반환 (잘못된 요청)
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 취소할 수 없는 상태의 주문 (준비 중인 주문)")
	void cancelOrder_CannotCancel_PreparingOrder() throws Exception {
		// Given: PREPARING 상태인 주문에 대한 취소 요청
		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenThrow(OrderException.invalidStatusTransition());

		// When & Then: 400 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 잘못된 주문 ID 형식")
	void cancelOrder_InvalidOrderIdFormat() throws Exception {
		// Given: 잘못된 UUID 형식의 주문 ID
		String invalidOrderId = "invalid-uuid";

		// When & Then: 400 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", invalidOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("실패: 서버 내부 오류")
	void cancelOrder_InternalServerError() throws Exception {
		// Given: 서비스에서 예상하지 못한 오류 발생
		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenThrow(new RuntimeException("Unexpected error"));

		// When & Then: 500 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError());
	}

	/**
	 * 테스트용 Mock 주문 객체 생성 헬퍼 메서드
	 */
	private Order createMockOrder(UUID orderId, OrderStatus status) {
		return Order.builder()
				.id(orderId)
				.orderNo("O20261203-000001")
				.customerId(1001L)
				.storeId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.orderType(OrderType.RESERVATION)
				.status(status)
				.totalAmount(50000)
				.cancelableUntil(LocalDateTime.now().plusMinutes(5))
				.build();
	}
}