package com.popcorn.demo.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.order.dto.request.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.request.OrderItemRequest;
import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.dto.response.CancelOrderResponse;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse.OrderItemResponse;
import com.popcorn.demo.domain.order.dto.response.OrderCreatedDto;
import com.popcorn.demo.domain.order.dto.response.UpdateOrderStatusResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

class OrderCommandControllerTest {

	@Test
	@DisplayName("주문 생성 응답을 반환한다")
	void createOrderReturnsResponse() {
		OrderCommandService service = Mockito.mock(OrderCommandService.class);
		OrderCommandController controller = new OrderCommandController(service, new ObjectMapper());

		UUID orderId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(orderId)
				.orderNo("O-1001")
				.orderType("RESERVATION")
				.status("REQUESTED")
				.storeId(UUID.randomUUID())
				.popupId(popupId)
				.totalAmount(10000)
				.cancelableUntil(LocalDateTime.now().plusMinutes(10))
				.createdAt(LocalDateTime.now())
				.items(List.of(OrderItemResponse.builder()
						.itemId(UUID.randomUUID())
						.orderItemType("RESERVATION")
						.qty(1)
						.unitPrice(10000)
						.lineAmount(10000)
						.build()))
				.build();
		when(service.createOrder(Mockito.any())).thenReturn(response);

		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.popupId(popupId)
				.items(List.of(OrderItemRequest.builder()
						.orderItemType("RESERVATION")
						.sessionId(UUID.randomUUID())
						.qty(1)
						.build()))
				.build();

		Authentication authentication = authentication(1001L, UserRole.CUSTOMER);
		ResponseEntity<BaseResponse<OrderCreatedDto>> result =
				controller.createOrder(request, authentication);

		assertThat(result.getBody().getData().getOrderId()).isEqualTo(orderId);
	}

	@Test
	@DisplayName("주문 상태 변경을 처리한다")
	void updateOrderStatusReturnsResponse() {
		OrderCommandService service = Mockito.mock(OrderCommandService.class);
		OrderCommandController controller = new OrderCommandController(service, new ObjectMapper());

		UUID orderId = UUID.randomUUID();
		Order order = Order.builder()
				.id(orderId)
				.status(OrderStatus.ACCEPTED)
				.build();
		order.setUpdatedAt(LocalDateTime.now());
		when(service.updateStatus(orderId, "ACCEPTED", "reason")).thenReturn(order);

		UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
				.status("ACCEPTED")
				.reason("reason")
				.build();

		ResponseEntity<BaseResponse<UpdateOrderStatusResponse>> response =
				controller.updateOrderStatus(orderId, request);

		assertThat(response.getBody().getData().getStatus()).isEqualTo("ACCEPTED");
	}

	@Test
	@DisplayName("주문 취소 응답을 반환한다")
	void cancelOrderReturnsResponse() {
		OrderCommandService service = Mockito.mock(OrderCommandService.class);
		OrderCommandController controller = new OrderCommandController(service, new ObjectMapper());

		UUID orderId = UUID.randomUUID();
		Order cancelled = Order.builder().id(orderId).status(OrderStatus.CANCELLED).build();
		when(service.updateStatus(orderId, OrderStatus.CANCELLED.name(), "고객 요청에 의한 취소"))
				.thenReturn(cancelled);

		Authentication authentication = authentication(1001L, UserRole.CUSTOMER);
		ResponseEntity<BaseResponse<CancelOrderResponse>> response =
				controller.cancelOrder(orderId, authentication);

		assertThat(response.getBody().getData().getStatus()).isEqualTo("CANCELED");
	}

	@Test
	@DisplayName("모든 주문 삭제 요청을 전달한다")
	void deleteAllOrdersDelegates() {
		OrderCommandService service = Mockito.mock(OrderCommandService.class);
		OrderCommandController controller = new OrderCommandController(service, new ObjectMapper());

		controller.deleteAllOrders();

		verify(service).deleteAllOrders();
	}

	private Authentication authentication(Long userId, UserRole role) {
		User user = Mockito.mock(User.class);
		when(user.getUserId()).thenReturn(userId);
		when(user.getRole()).thenReturn(role);
		CustomUserDetails userDetails = new CustomUserDetails(user);
		Authentication authentication = Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		return authentication;
	}
}
