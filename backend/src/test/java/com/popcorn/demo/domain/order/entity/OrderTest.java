package com.popcorn.demo.domain.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

	@Test
	@DisplayName("주문 번호 형식에 접두사와 날짜 포함")
	void orderNumberFormat() {
		String orderNo = Order.generateOrderNo();

		assertThat(orderNo)
				.startsWith("O")
				.contains("-")
				.hasSize(16);
	}

	@Test
	@DisplayName("타입 확인 및 취소 가능 로직")
	void typeChecksAndCancelableLogic() {
		UUID storeId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		Order reservationOrder = Order.builder()
				.orderNo("O-1")
				.customerId(1L)
				.storeId(storeId)
				.productId(productId)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.cancelableUntil(LocalDateTime.now().plusMinutes(10))
				.build();

		Order purchaseOrder = Order.builder()
				.orderNo("O-2")
				.customerId(1L)
				.storeId(storeId)
				.productId(productId)
				.orderType(OrderType.PURCHASE)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.cancelableUntil(LocalDateTime.now().minusMinutes(10))
				.build();

		assertThat(reservationOrder.isReservationType()).isTrue();
		assertThat(reservationOrder.isPurchaseType()).isFalse();
		assertThat(reservationOrder.isCancelable()).isTrue();

		assertThat(purchaseOrder.isReservationType()).isFalse();
		assertThat(purchaseOrder.isPurchaseType()).isTrue();
		assertThat(purchaseOrder.isCancelable()).isFalse();
	}

	@Test
	@DisplayName("총 수량은 아이템 수량 합계")
	void totalQuantitySumsItems() {
		UUID storeId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		Order order = Order.builder()
				.orderNo("O-3")
				.customerId(1L)
				.storeId(storeId)
				.productId(productId)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.build();

		OrderItem item1 = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(UUID.randomUUID())
				.qty(2)
				.unitPrice(1000)
				.lineAmount(2000)
				.build();

		OrderItem item2 = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(UUID.randomUUID())
				.qty(3)
				.unitPrice(1000)
				.lineAmount(3000)
				.build();

		order.setOrderItems(List.of(item1, item2));

		assertThat(order.getTotalQuantity()).isEqualTo(5);
	}
}
