package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderConflictException;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;

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
class OrderCancelControllerTest extends OrderControllerTestBase {

	private final UUID testOrderId = UUID.fromString("00000000-0000-0000-0000-000000001001");

	@Test
	@DisplayName("성공: 정상적인 주문 취소")
	void cancelOrder_Success() throws Exception {
		// Given: REQUESTED 상태의 주문이 있고, 취소 요청이 들어왔을 때
		Order existingOrder = createMockOrder(testOrderId, OrderStatus.REQUESTED);
		Order cancelledOrder = createMockOrder(testOrderId, OrderStatus.CANCELLED);

		// orderRepository.findById() mocking 추가
		when(orderRepository.findById(testOrderId))
				.thenReturn(Optional.of(existingOrder));

		// orderDomainService.canChangeStatus() mocking 추가
		when(orderDomainService.canChangeStatus(OrderStatus.REQUESTED, OrderStatus.CANCELLED))
				.thenReturn(true);

		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenReturn(cancelledOrder);

		// When: 주문 취소 API 호출
		// Then: 200 응답과 함께 취소된 주문 정보 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
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

		// orderRepository.findById() mocking 추가 - empty Optional 반환
		when(orderRepository.findById(nonExistentOrderId))
				.thenReturn(Optional.empty());

		// When & Then: 404 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", nonExistentOrderId)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 이미 취소된 주문")
	void cancelOrder_AlreadyCancelled() throws Exception {
		// Given: 이미 CANCELLED 상태인 주문
		Order alreadyCancelledOrder = createMockOrder(testOrderId, OrderStatus.CANCELLED);

		// orderRepository.findById() mocking 추가
		when(orderRepository.findById(testOrderId))
				.thenReturn(Optional.of(alreadyCancelledOrder));

		// orderDomainService.canChangeStatus() mocking 추가 - 불가능 반환
		when(orderDomainService.canChangeStatus(OrderStatus.CANCELLED, OrderStatus.CANCELLED))
				.thenReturn(false);

		// When & Then: 400 응답 반환 (잘못된 상태 전이)
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 취소할 수 없는 상태의 주문 (완료된 주문)")
	void cancelOrder_CannotCancel_CompletedOrder() throws Exception {
		// Given: COMPLETED 상태인 주문에 대한 취소 요청
		Order completedOrder = createMockOrder(testOrderId, OrderStatus.COMPLETED);

		// orderRepository.findById() mocking 추가
		when(orderRepository.findById(testOrderId))
				.thenReturn(Optional.of(completedOrder));

		// orderDomainService.canChangeStatus() mocking 추가 - 불가능 반환
		when(orderDomainService.canChangeStatus(OrderStatus.COMPLETED, OrderStatus.CANCELLED))
				.thenReturn(false);

		// When & Then: 400 응답 반환 (잘못된 요청)
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	@DisplayName("실패: 취소할 수 없는 상태의 주문 (준비 중인 주문)")
	void cancelOrder_CannotCancel_PreparingOrder() throws Exception {
		// Given: PAYMENT_PENDING 상태인 주문에 대한 취소 요청
		Order preparingOrder = createMockOrder(testOrderId, OrderStatus.PAYMENT_PENDING);

		// orderRepository.findById() mocking 추가
		when(orderRepository.findById(testOrderId))
				.thenReturn(Optional.of(preparingOrder));

		// orderDomainService.canChangeStatus() mocking 추가 - 불가능 반환
		when(orderDomainService.canChangeStatus(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED))
				.thenReturn(false);

		// When & Then: 400 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
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
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("실패: 서버 내부 오류")
	void cancelOrder_InternalServerError() throws Exception {
		// Given: 서비스에서 예상하지 못한 오류 발생
		Order existingOrder = createMockOrder(testOrderId, OrderStatus.REQUESTED);

		// orderRepository.findById() mocking 추가
		when(orderRepository.findById(testOrderId))
				.thenReturn(Optional.of(existingOrder));

		// orderDomainService.canChangeStatus() mocking 추가 - 가능 반환
		when(orderDomainService.canChangeStatus(OrderStatus.REQUESTED, OrderStatus.CANCELLED))
				.thenReturn(true);

		when(orderCommandService.updateStatus(eq(testOrderId), eq("CANCELLED"), any(String.class)))
				.thenThrow(new RuntimeException("Unexpected error"));

		// When & Then: 500 응답 반환
		mockMvc.perform(delete("/api/v1/orders/{orderId}/cancel", testOrderId)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(createCustomerAuthentication()))
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
				.popupId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.orderType(OrderType.RESERVATION)
				.status(status)
				.totalAmount(50000)
				.cancelableUntil(LocalDateTime.now().plusMinutes(5))
				.build();
	}
}
