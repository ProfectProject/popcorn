package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderForbiddenException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;

class OrderAuthorizationServiceTest {

	@Test
	@DisplayName("역할별 주문 접근 검증이 동작한다")
	void validateAccessByRole() {
		OrderAuthorizationService service = new OrderAuthorizationService(Mockito.mock(OrderQueryRepository.class));
		UUID orderId = UUID.randomUUID();

		service.validateOrderAccess(orderId, 10L, "OWNER");
		service.validateOrderAccess(orderId, 10L, "MANAGER");
		service.validateOrderAccess(orderId, 10L, "CUSTOMER");

		assertThatThrownBy(() -> service.validateOrderAccess(orderId, null, "OWNER"))
				.isInstanceOf(OrderForbiddenException.class);
		assertThatThrownBy(() -> service.validateOrderAccess(orderId, 10L, "UNKNOWN"))
				.isInstanceOf(OrderForbiddenException.class);
	}

	@Test
	@DisplayName("상태 변경 권한과 취소 권한을 검증한다")
	void validateStatusChangeAndCancellation() {
		OrderAuthorizationService service = new OrderAuthorizationService(Mockito.mock(OrderQueryRepository.class));
		UUID orderId = UUID.randomUUID();

		service.validateStatusChangePermission(orderId, 1L, "OWNER",
				OrderStatus.REQUESTED, OrderStatus.ACCEPTED);

		assertThatThrownBy(() -> service.validateStatusChangePermission(orderId, 1L, "CUSTOMER",
				OrderStatus.REQUESTED, OrderStatus.ACCEPTED))
				.isInstanceOf(OrderForbiddenException.class);

		Order completedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.COMPLETED)
				.build();

		assertThatThrownBy(() -> service.validateOrderCancellationPermission(completedOrder, 1L, "OWNER"))
				.isInstanceOf(OrderValidationException.class);

		Order requestedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.REQUESTED)
				.build();

		service.validateOrderCancellationPermission(requestedOrder, 1L, "CUSTOMER");
		assertThatThrownBy(() -> service.validateOrderCancellationPermission(
				Order.builder().id(orderId).status(OrderStatus.ACCEPTED).build(), 1L, "CUSTOMER"))
				.isInstanceOf(OrderForbiddenException.class);
	}
}
