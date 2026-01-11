package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;

class OrderBusinessRuleServiceTest {

	private final OrderBusinessRuleService service = new OrderBusinessRuleService();

	@Test
	@DisplayName("상태 전이 규칙과 사유 필수 규칙이 적용된다")
	void validateStatusTransitionRules() {
		service.validateStatusTransitionRules(OrderStatus.REQUESTED, OrderStatus.ACCEPTED, "ok");

		assertThatThrownBy(() -> service.validateStatusTransitionRules(
				OrderStatus.REQUESTED, OrderStatus.COMPLETED, ""))
				.isInstanceOf(OrderValidationException.class);

		assertThatThrownBy(() -> service.validateStatusTransitionRules(
				OrderStatus.REQUESTED, OrderStatus.CANCELLED, ""))
				.isInstanceOf(OrderValidationException.class);
	}

	@Test
	@DisplayName("취소 규칙에서 완료/시간 초과를 차단한다")
	void validateCancellationRules() {
		assertThatThrownBy(() -> service.validateOrderCancellationRules(
				OrderStatus.COMPLETED, LocalDateTime.now()))
				.isInstanceOf(OrderValidationException.class);

		assertThatThrownBy(() -> service.validateOrderCancellationRules(
				OrderStatus.REQUESTED, LocalDateTime.now().minusHours(1)))
				.isInstanceOf(OrderValidationException.class);
	}

	@Test
	@DisplayName("아이템/금액/조합 규칙이 반영된다")
	void validateOrderItemRules() {
		CreateOrderCommand.OrderItemCommand reservationItem = CreateOrderCommand.OrderItemCommand.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionId(UUID.randomUUID())
				.qty(1)
				.unitPrice(1000)
				.build();

		ReflectionTestUtils.invokeMethod(service, "validateOrderItems", List.of(reservationItem));
		ReflectionTestUtils.invokeMethod(service, "validateOrderAmount", List.of(reservationItem));
		ReflectionTestUtils.invokeMethod(service, "validateOrderItemCombination", "RESERVATION",
				List.of(reservationItem));

		CreateOrderCommand.OrderItemCommand missingSession = CreateOrderCommand.OrderItemCommand.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionId(null)
				.qty(1)
				.unitPrice(1000)
				.build();

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
				service, "validateOrderItems", List.of(missingSession)))
				.isInstanceOf(OrderNotFoundException.class);

		CreateOrderCommand.OrderItemCommand goodsItem = CreateOrderCommand.OrderItemCommand.builder()
				.orderItemType(OrderItemType.GOODS)
				.goodsVariantId(UUID.randomUUID())
				.qty(1)
				.unitPrice(1000)
				.build();

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
				service, "validateOrderItemCombination", "RESERVATION", List.of(goodsItem)))
				.isInstanceOf(OrderValidationException.class);
	}
}
