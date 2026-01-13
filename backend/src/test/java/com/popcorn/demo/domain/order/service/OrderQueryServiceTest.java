package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.config.OrderProperties;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;
import com.popcorn.demo.domain.order.repository.view.OrderAddressView;
import com.popcorn.demo.domain.order.repository.view.OrderDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderItemDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderPaymentView;
import com.popcorn.demo.domain.order.repository.view.OrderStatusView;
import com.popcorn.demo.domain.order.repository.view.OrderTimelineView;
import com.popcorn.demo.domain.order.repository.view.StoreOrderReservationView;

class OrderQueryServiceTest {

	@Test
	@DisplayName("Store order list returns empty when no results")
	void getStoreOrderReservationsReturnsEmptyWhenNoResults() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		UUID storeId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		when(repository.countStoreOrders(storeId, popupId, null, null, null, null, null)).thenReturn(0L);

		StoreOrderReservationListResponse response = service.getStoreOrderReservations(
				storeId, popupId, null, null, null, null, null, null, null);

		assertThat(response.getItems()).isEmpty();
		assertThat(response.getTotal()).isZero();
	}

	@Test
	@DisplayName("Store order list maps view data")
	void getStoreOrderReservationsMapsViews() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		UUID storeId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		when(repository.countStoreOrders(storeId, popupId, null, null, "REQUESTED", null, null)).thenReturn(2L);
		List<StoreOrderReservationView> views = List.of(
				storeOrderView(UUID.randomUUID(), "O-1"),
				storeOrderView(UUID.randomUUID(), "O-2")
		);
		when(repository.findStoreOrders(storeId, popupId, null, null, "REQUESTED", null, null, 5, 0L))
				.thenReturn(views);

		StoreOrderReservationListResponse response = service.getStoreOrderReservations(
				storeId, popupId, null, null, "REQUESTED", null, null, 5, 0L);

		assertThat(response.getItems()).hasSize(2);
		assertThat(response.getSize()).isEqualTo(5);
	}

	@Test
	@DisplayName("Customer timeline returns empty when no results")
	void getMyOrderTimelineReturnsEmptyWhenNoResults() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		when(repository.countCustomerOrders(1001L, null, null, null, null)).thenReturn(0L);

		MyOrderTimelineResponse response = service.getMyOrderTimeline(
				null, "ALL", "", null, null, null, null);

		assertThat(response.getItems()).isEmpty();
		assertThat(response.getTotal()).isZero();
	}

	@Test
	@DisplayName("Customer timeline maps location data")
	void getMyOrderTimelineMapsViewsWithLocation() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		when(repository.countCustomerOrders(1001L, "RESERVATION", "PAID", null, null)).thenReturn(1L);
		OrderTimelineView view = orderTimelineView(UUID.randomUUID(), "O-2001");
		when(repository.findCustomerOrders(1001L, "RESERVATION", "PAID", null, null, 10, 0L))
				.thenReturn(List.of(view));

		MyOrderTimelineResponse response = service.getMyOrderTimeline(
				1001L, "reservation", "paid", null, null, 10, 0L);

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getLocation()).isNotNull();
	}

	@Test
	@DisplayName("Customer order status uses customer scope")
	void getOrderStatusForCustomerReturnsStatus() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		UUID orderId = UUID.randomUUID();
		OrderStatusView view = orderStatusView(orderId, "O-1001");
		when(repository.findOrderStatus(orderId, 1001L)).thenReturn(view);

		OrderStatusDto response = service.getOrderStatusForCustomer(orderId, 1001L);

		assertThat(response.getOrderNo()).isEqualTo("O-1001");
	}

	@Test
	@DisplayName("Order status throws when missing")
	void getOrderStatusForCustomerThrowsWhenMissing() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		UUID orderId = UUID.randomUUID();
		when(repository.findOrderStatus(orderId, 1001L)).thenReturn(null);

		assertThatThrownBy(() -> service.getOrderStatusForCustomer(orderId, 1001L))
				.isInstanceOf(OrderNotFoundException.class);
	}

	@Test
	@DisplayName("Staff order status validates access")
	void getOrderStatusForStaffValidatesAccess() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = createService(repository, authorizationService);

		UUID orderId = UUID.randomUUID();
		OrderStatusView view = orderStatusView(orderId, "O-2001");
		when(repository.findOrderStatusByOrderId(orderId)).thenReturn(view);

		OrderStatusDto response = service.getOrderStatusForStaff(orderId, 2001L, "OWNER");

		assertThat(response.getOrderNo()).isEqualTo("O-2001");
		verify(authorizationService).validateOrderAccess(orderId, 2001L, "OWNER");
	}

	@Test
	@DisplayName("Order detail includes items address and payment")
	void getOrderDetailMapsDetail() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = createService(repository, authorizationService);

		UUID orderId = UUID.randomUUID();
		OrderDetailView view = orderDetailView(orderId, "O-3001");
		OrderItemDetailView itemView = orderItemDetailView();
		OrderAddressView addressView = orderAddressView();
		OrderPaymentView paymentView = orderPaymentView();
		when(repository.findOrderDetail(orderId)).thenReturn(view);
		when(repository.findOrderItems(orderId, view.getPopupId())).thenReturn(List.of(itemView));
		when(repository.findDefaultAddress(view.getCustomerId())).thenReturn(addressView);
		when(repository.findPayment(orderId)).thenReturn(paymentView);

		OrderDetailDto response = service.getOrderDetail(orderId, 1001L, "CUSTOMER");

		assertThat(response.getOrderNo()).isEqualTo("O-3001");
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getAddress()).isNotNull();
		assertThat(response.getPayment()).isNotNull();
		verify(authorizationService).validateOrderAccess(orderId, 1001L, "CUSTOMER");
	}

	@Test
	@DisplayName("Order detail returns null for missing optional data")
	void getOrderDetailHandlesMissingOptionalData() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		UUID orderId = UUID.randomUUID();
		OrderDetailView view = orderDetailView(orderId, "O-4001");
		when(repository.findOrderDetail(orderId)).thenReturn(view);
		when(repository.findOrderItems(orderId, view.getPopupId())).thenReturn(List.of());
		when(repository.findDefaultAddress(view.getCustomerId())).thenReturn(null);
		when(repository.findPayment(orderId)).thenReturn(null);

		OrderDetailDto response = service.getOrderDetail(orderId, null, null);

		assertThat(response.getAddress()).isNull();
		assertThat(response.getPayment()).isNull();
	}

	@Test
	@DisplayName("Complete detail delegates to batch service")
	void getCompleteOrderDetailDelegates() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderBatchQueryService batchQueryService = Mockito.mock(OrderBatchQueryService.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = createService(repository, batchQueryService, authorizationService);

		UUID orderId = UUID.randomUUID();
		OrderDetailDto expected = OrderDetailDto.builder().id(orderId).orderNo("O-5001").build();
		when(batchQueryService.getCompleteOrderDetail(orderId)).thenReturn(expected);

		OrderDetailDto response = service.getCompleteOrderDetail(orderId, 1L, "OWNER");

		assertThat(response.getOrderNo()).isEqualTo("O-5001");
		verify(authorizationService).validateOrderAccess(orderId, 1L, "OWNER");
	}

	@Test
	@DisplayName("Batch detail returns empty when no ids")
	void getBatchOrderDetailsReturnsEmptyWhenNoIds() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderQueryService service = createService(repository);

		Map<UUID, OrderDetailDto> response = service.getBatchOrderDetails(Set.of(), null, null);

		assertThat(response).isEmpty();
	}

	@Test
	@DisplayName("Batch detail validates access and logs metrics")
	void getBatchOrderDetailsValidatesAccess() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderBatchQueryService batchQueryService = Mockito.mock(OrderBatchQueryService.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = createService(repository, batchQueryService, authorizationService);

		UUID orderId = UUID.randomUUID();
		Map<UUID, OrderDetailDto> expected = Map.of(orderId, OrderDetailDto.builder().id(orderId).build());
		when(batchQueryService.getBatchOrderDetails(Set.of(orderId))).thenReturn(expected);

		Map<UUID, OrderDetailDto> response = service.getBatchOrderDetails(Set.of(orderId), 1L, "OWNER");

		assertThat(response).containsKey(orderId);
		verify(authorizationService).validateOrderAccess(orderId, 1L, "OWNER");
		verify(batchQueryService).logBatchPerformanceMetrics(Mockito.eq(Set.of(orderId)), Mockito.anyLong());
	}

	private OrderQueryService createService(OrderQueryRepository repository) {
		return createService(repository, Mockito.mock(OrderBatchQueryService.class), Mockito.mock(OrderAuthorizationService.class));
	}

	private OrderQueryService createService(OrderQueryRepository repository, OrderAuthorizationService authorizationService) {
		return createService(repository, Mockito.mock(OrderBatchQueryService.class), authorizationService);
	}

	private OrderQueryService createService(OrderQueryRepository repository,
			OrderBatchQueryService batchQueryService,
			OrderAuthorizationService authorizationService) {
		OrderProperties properties = new OrderProperties();
		properties.getPagination().setDefaultSize(20);
		properties.getPagination().setCustomerOrderMaxSize(50);
		properties.getPagination().setStoreOrderMaxSize(100);
		return new OrderQueryService(repository, properties, batchQueryService, authorizationService);
	}

	private StoreOrderReservationView storeOrderView(UUID id, String orderNo) {
		StoreOrderReservationView view = Mockito.mock(StoreOrderReservationView.class);
		when(view.getId()).thenReturn(id);
		when(view.getOrderNo()).thenReturn(orderNo);
		when(view.getStatus()).thenReturn("REQUESTED");
		when(view.getTotalAmount()).thenReturn(10000);
		when(view.getCancelableUntil()).thenReturn(LocalDateTime.now().plusDays(1));
		when(view.getCreatedAt()).thenReturn(LocalDateTime.now().minusHours(1));
		return view;
	}

	private OrderTimelineView orderTimelineView(UUID id, String orderNo) {
		OrderTimelineView view = Mockito.mock(OrderTimelineView.class);
		when(view.getOrderType()).thenReturn("RESERVATION");
		when(view.getId()).thenReturn(id);
		when(view.getOrderNo()).thenReturn(orderNo);
		when(view.getStatus()).thenReturn("REQUESTED");
		when(view.getTotalAmount()).thenReturn(12000);
		when(view.getCancelableUntil()).thenReturn(LocalDateTime.now().plusDays(1));
		when(view.getCreatedAt()).thenReturn(LocalDateTime.now().minusHours(2));
		when(view.getPopupId()).thenReturn(UUID.randomUUID());
		when(view.getStoreId()).thenReturn(UUID.randomUUID());
		when(view.getProductTitle()).thenReturn("Popup");
		when(view.getSessionStartAt()).thenReturn(LocalDateTime.now().plusDays(2));
		when(view.getLocationName()).thenReturn("Location");
		when(view.getLocationAddress1()).thenReturn("Address1");
		when(view.getLocationAddress2()).thenReturn("Address2");
		return view;
	}

	private OrderStatusView orderStatusView(UUID orderId, String orderNo) {
		OrderStatusView view = Mockito.mock(OrderStatusView.class);
		when(view.getOrderId()).thenReturn(orderId);
		when(view.getOrderNo()).thenReturn(orderNo);
		when(view.getStatus()).thenReturn("REQUESTED");
		when(view.getPaymentStatus()).thenReturn("PAID");
		when(view.getCancelableUntil()).thenReturn(LocalDateTime.now().plusDays(1));
		when(view.getUpdatedAt()).thenReturn(LocalDateTime.now());
		return view;
	}

	private OrderDetailView orderDetailView(UUID orderId, String orderNo) {
		OrderDetailView view = Mockito.mock(OrderDetailView.class);
		when(view.getOrderId()).thenReturn(orderId);
		when(view.getOrderNo()).thenReturn(orderNo);
		when(view.getOrderType()).thenReturn("RESERVATION");
		when(view.getStatus()).thenReturn("REQUESTED");
		when(view.getCustomerId()).thenReturn(1001L);
		when(view.getCustomerRole()).thenReturn("CUSTOMER");
		when(view.getCustomerPhone()).thenReturn("01000000000");
		when(view.getStoreId()).thenReturn(UUID.randomUUID());
		when(view.getStoreOwnerId()).thenReturn(10L);
		when(view.getPopupId()).thenReturn(UUID.randomUUID());
		when(view.getTotalAmount()).thenReturn(30000);
		when(view.getCancelableUntil()).thenReturn(LocalDateTime.now().plusDays(1));
		when(view.getCreatedAt()).thenReturn(LocalDateTime.now().minusHours(3));
		when(view.getUpdatedAt()).thenReturn(LocalDateTime.now().minusHours(1));
		return view;
	}

	private OrderItemDetailView orderItemDetailView() {
		OrderItemDetailView view = Mockito.mock(OrderItemDetailView.class);
		when(view.getOrderItemId()).thenReturn(UUID.randomUUID());
		when(view.getOrderItemType()).thenReturn("RESERVATION");
		when(view.getPopupId()).thenReturn(UUID.randomUUID());
		when(view.getProductTitle()).thenReturn("Popup");
		when(view.getProductCategory()).thenReturn("CATEGORY");
		when(view.getProductStatus()).thenReturn("OPEN");
		when(view.getSessionId()).thenReturn(UUID.randomUUID());
		when(view.getSessionStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
		when(view.getSessionEndAt()).thenReturn(LocalDateTime.now().plusDays(1).plusHours(1));
		when(view.getGoodsVariantId()).thenReturn(UUID.randomUUID());
		when(view.getMerchVariantName()).thenReturn("Variant");
		when(view.getMerchSku()).thenReturn("SKU-1");
		when(view.getQty()).thenReturn(1);
		when(view.getUnitPrice()).thenReturn(10000);
		when(view.getLineAmount()).thenReturn(10000);
		return view;
	}

	private OrderAddressView orderAddressView() {
		OrderAddressView view = Mockito.mock(OrderAddressView.class);
		when(view.getAddress1()).thenReturn("Address1");
		when(view.getAddress2()).thenReturn("Address2");
		when(view.getReceiverName()).thenReturn("Receiver");
		when(view.getPhone()).thenReturn("01000000000");
		return view;
	}

	private OrderPaymentView orderPaymentView() {
		OrderPaymentView view = Mockito.mock(OrderPaymentView.class);
		when(view.getPaymentId()).thenReturn(UUID.randomUUID());
		when(view.getMethod()).thenReturn("CARD");
		when(view.getStatus()).thenReturn("COMPLETED");
		when(view.getAmount()).thenReturn(30000);
		when(view.getApprovedAt()).thenReturn(LocalDateTime.now());
		return view;
	}
}
