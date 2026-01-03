package com.popcorn.demo.domain.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.config.OrderProperties;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;
import com.popcorn.demo.domain.order.repository.view.OrderStatusView;

class OrderQueryServiceTest {

	@Test
	@DisplayName("주문 상태 조회 - customerId 없으면 forbidden")
	void getOrderStatusForCustomer_throwsWhenCustomerIdMissing() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderProperties properties = new OrderProperties();
		OrderBatchQueryService batchQueryService = Mockito.mock(OrderBatchQueryService.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = new OrderQueryService(repository, properties, batchQueryService, authorizationService);

		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		OrderException exception = assertThrows(OrderException.class,
				() -> service.getOrderStatusForCustomer(orderId, null));

		assertEquals(CommonResponseCode.FORBIDDEN, exception.getResponseCode());
	}

	@Test
	@DisplayName("주문 상태 조회 - 주문 없으면 not found")
	void getOrderStatusForCustomer_throwsWhenNotFound() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderProperties properties = new OrderProperties();
		OrderBatchQueryService batchQueryService = Mockito.mock(OrderBatchQueryService.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = new OrderQueryService(repository, properties, batchQueryService, authorizationService);

		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		when(repository.findOrderStatus(eq(orderId), eq(1001L))).thenReturn(null);

		OrderException exception = assertThrows(OrderException.class,
				() -> service.getOrderStatusForCustomer(orderId, 1001L));

		assertEquals(OrderResponseCode.ORDER_NOT_FOUND, exception.getResponseCode());
	}

	@Test
	@DisplayName("주문 상태 조회 - 정상 매핑")
	void getOrderStatusForCustomer_mapsFields() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderProperties properties = new OrderProperties();
		OrderBatchQueryService batchQueryService = Mockito.mock(OrderBatchQueryService.class);
		OrderAuthorizationService authorizationService = Mockito.mock(OrderAuthorizationService.class);
		OrderQueryService service = new OrderQueryService(repository, properties, batchQueryService, authorizationService);

		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		LocalDateTime cancelableUntil = LocalDateTime.of(2025, 1, 1, 10, 30);
		LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 10, 5);

		when(repository.findOrderStatus(eq(orderId), eq(1001L)))
				.thenReturn(new OrderStatusView() {
					@Override
					public UUID getOrderId() {
						return orderId;
					}

					@Override
					public String getOrderNo() {
						return "O20251231-001001";
					}

					@Override
					public String getStatus() {
						return "REQUESTED";
					}

					@Override
					public String getPaymentStatus() {
						return "READY";
					}

					@Override
					public LocalDateTime getCancelableUntil() {
						return cancelableUntil;
					}

					@Override
					public LocalDateTime getUpdatedAt() {
						return updatedAt;
					}
				});

		OrderStatusDto response = service.getOrderStatusForCustomer(orderId, 1001L);

		assertEquals(orderId, response.getOrderId());
		assertEquals("O20251231-001001", response.getOrderNo());
		assertEquals("REQUESTED", response.getStatus());
		assertEquals("READY", response.getPaymentStatus());
		assertEquals(cancelableUntil, response.getCancelableUntil());
		assertEquals(updatedAt, response.getUpdatedAt());
	}
}
