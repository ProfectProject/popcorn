package com.popcorn.demo.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.popcorn.common.dto.BaseResponse;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

class OrderQueryControllerTest {

	@Test
	@DisplayName("주문 상세 조회를 반환한다")
	void getOrderDetailReturnsResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		UUID orderId = UUID.randomUUID();
		OrderDetailDto detail = OrderDetailDto.builder()
				.id(orderId)
				.orderNo("O-1001")
				.build();
		when(service.getOrderDetail(orderId, 1001L, "CUSTOMER")).thenReturn(detail);

		ResponseEntity<BaseResponse<OrderDetailDto>> response =
				controller.getOrderDetail(orderId, authentication(1001L, UserRole.CUSTOMER));

		assertThat(response.getBody().getData().getOrderNo()).isEqualTo("O-1001");
	}

	@Test
	@DisplayName("주문 상태 조회를 반환한다")
	void getOrderStatusReturnsResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		UUID orderId = UUID.randomUUID();
		OrderStatusDto status = OrderStatusDto.builder()
				.orderId(orderId)
				.status("REQUESTED")
				.build();
		when(service.getOrderStatusForCustomer(orderId, 1001L)).thenReturn(status);

		ResponseEntity<BaseResponse<OrderStatusDto>> response =
				controller.getOrderStatus(orderId, authentication(1001L, UserRole.CUSTOMER));

		assertThat(response.getBody().getData().getStatus()).isEqualTo("REQUESTED");
	}

	@Test
	@DisplayName("운영자 주문 상태 조회를 반환한다")
	void getOrderStatusForStaffReturnsResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		UUID orderId = UUID.randomUUID();
		OrderStatusDto status = OrderStatusDto.builder()
				.orderId(orderId)
				.status("PAID")
				.build();
		when(service.getOrderStatusForStaff(orderId, 2001L, "OWNER")).thenReturn(status);

		ResponseEntity<BaseResponse<OrderStatusDto>> response =
				controller.getOrderStatusForStaff(orderId, authentication(2001L, UserRole.OWNER));

		assertThat(response.getBody().getData().getStatus()).isEqualTo("PAID");
	}

	@Test
	@DisplayName("스토어/팝업 없이 요청하면 예외가 발생한다")
	void getStoreOrderStatusesRequiresStoreOrPopup() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		assertThatThrownBy(() -> controller.getStoreOrderStatusesForStaff(null, null, null, null, null, 1, 10))
				.isInstanceOf(OrderValidationException.class);
	}

	@Test
	@DisplayName("스토어 주문 상태 목록을 반환한다")
	void getStoreOrderStatusesReturnsResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		StoreOrderReservationListResponse list = StoreOrderReservationListResponse.builder()
				.items(List.of())
				.page(0)
				.size(10)
				.total(0L)
				.build();
		when(service.getStoreOrderReservations(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.isNull(),
				Mockito.eq("REQUESTED"), Mockito.isNull(), Mockito.isNull(), Mockito.eq(10), Mockito.eq(0L)))
				.thenReturn(list);

		ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> response =
				controller.getStoreOrderStatusesForStaff(UUID.randomUUID(), null, null, null,
						OrderStatus.REQUESTED, 1, 10);

		assertThat(response.getBody().getData().getItems()).isEmpty();
	}

	@Test
	@DisplayName("스토어 주문 목록을 반환한다")
	void getStoreOrdersReturnsResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		StoreOrderReservationListResponse list = StoreOrderReservationListResponse.builder()
				.items(List.of())
				.page(0)
				.size(10)
				.total(0L)
				.build();
		when(service.getStoreOrderReservations(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.isNull(),
				Mockito.eq("REQUESTED"), Mockito.any(), Mockito.any(), Mockito.eq(10), Mockito.eq(0L)))
				.thenReturn(list);

		ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> response =
				controller.getStoreOrders(UUID.randomUUID(), UUID.randomUUID(), null, null, OrderStatus.REQUESTED,
						LocalDateTime.now().minusDays(1), LocalDateTime.now(), 1, 10);

		assertThat(response.getBody().getData().getItems()).isEmpty();
	}

	@Test
	@DisplayName("내 주문 타임라인을 반환한다")
	void getMyOrdersReturnsResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		MyOrderTimelineResponse timeline = MyOrderTimelineResponse.builder()
				.items(List.of())
				.page(0)
				.size(20)
				.total(0L)
				.build();
		when(service.getMyOrderTimeline(1001L, null, null, null, null, 20, 0L))
				.thenReturn(timeline);

		ResponseEntity<BaseResponse<MyOrderTimelineResponse>> response =
				controller.getMyOrders("ALL", "ALL", null, null, 1, 20,
						authentication(1001L, UserRole.CUSTOMER));

		assertThat(response.getBody().getData().getTotal()).isZero();
	}

	@Test
	@DisplayName("필터가 있는 주문 타임라인을 반환한다")
	void getMyOrdersReturnsFilteredResponse() {
		OrderQueryService service = Mockito.mock(OrderQueryService.class);
		OrderQueryController controller = new OrderQueryController(service);

		MyOrderTimelineResponse timeline = MyOrderTimelineResponse.builder()
				.items(List.of())
				.page(0)
				.size(20)
				.total(0L)
				.build();
		when(service.getMyOrderTimeline(1001L, "RESERVATION", "PAID", null, null, 20, 0L))
				.thenReturn(timeline);

		ResponseEntity<BaseResponse<MyOrderTimelineResponse>> response =
				controller.getMyOrders("RESERVATION", "PAID", null, null, 1, 20,
						authentication(1001L, UserRole.CUSTOMER));

		assertThat(response.getBody().getData().getItems()).isEmpty();
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
