package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.config.OrderProperties;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;
import com.popcorn.demo.domain.order.repository.view.OrderDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderItemDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderPaymentView;
import com.popcorn.demo.domain.order.repository.view.OrderAddressView;

class OrderBatchQueryServiceTest {

	@Test
	@DisplayName("동기 배치 조회는 주문 정보가 없으면 빈 DTO를 반환한다")
	void getCompleteOrderDetailSync_empty() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderProperties properties = new OrderProperties();
		properties.getAsync().setParallelValidation(false);
		Executor executor = Runnable::run;

		OrderBatchQueryService service = new OrderBatchQueryService(repository, properties, executor);

		when(repository.findOrderDetail(any(UUID.class))).thenReturn(null);

		assertThat(service.getCompleteOrderDetail(UUID.randomUUID())).isNotNull();
	}

	@Test
	@DisplayName("비동기 배치 조회는 정상 데이터 조합 경로를 실행한다")
	void getCompleteOrderDetailAsync_success() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderProperties properties = new OrderProperties();
		properties.getAsync().setParallelValidation(true);
		Executor executor = Runnable::run;

		OrderBatchQueryService service = new OrderBatchQueryService(repository, properties, executor);
		UUID orderId = UUID.randomUUID();

		OrderDetailView orderDetail = new OrderDetailView() {
			@Override public UUID getOrderId() { return orderId; }
			@Override public String getOrderNo() { return "O-1"; }
			@Override public String getOrderType() { return "RESERVATION"; }
			@Override public String getStatus() { return "REQUESTED"; }
			@Override public Long getCustomerId() { return 1001L; }
			@Override public String getCustomerRole() { return "CUSTOMER"; }
			@Override public String getCustomerPhone() { return "010"; }
			@Override public UUID getStoreId() { return UUID.randomUUID(); }
			@Override public Long getStoreOwnerId() { return 2001L; }
			@Override public UUID getPopupId() { return UUID.randomUUID(); }
			@Override public Integer getTotalAmount() { return 1000; }
			@Override public LocalDateTime getCancelableUntil() { return LocalDateTime.now(); }
			@Override public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
			@Override public LocalDateTime getUpdatedAt() { return LocalDateTime.now(); }
		};

		when(repository.findOrderDetail(orderId)).thenReturn(orderDetail);
		when(repository.findOrderItems(orderId, orderId)).thenReturn(List.of(Mockito.mock(OrderItemDetailView.class)));
		when(repository.findPayment(orderId)).thenReturn(Mockito.mock(OrderPaymentView.class));
		when(repository.findDefaultAddress(1001L)).thenReturn(Mockito.mock(OrderAddressView.class));

		assertThat(service.getCompleteOrderDetail(orderId)).isNotNull();
	}

	@Test
	@DisplayName("배치 조회는 최대 크기 제한을 적용한다")
	void getBatchOrderDetails_withLimit() {
		OrderQueryRepository repository = Mockito.mock(OrderQueryRepository.class);
		OrderProperties properties = new OrderProperties();
		properties.getPagination().setMaxSize(1);
		Executor executor = Runnable::run;

		OrderBatchQueryService service = new OrderBatchQueryService(repository, properties, executor);

		UUID orderId = UUID.randomUUID();
		when(repository.findOrderDetail(orderId)).thenReturn(null);

		Map<UUID, ?> result = service.getBatchOrderDetails(Set.of(orderId, UUID.randomUUID()));
		assertThat(result).hasSize(1);
	}
}
