package com.popcorn.demo.domain.order.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateOrderRequestTest {

	@Test
	@DisplayName("예약 타입 검증 및 총 수량 계산")
	void reservationTypeChecksAndTotalQuantity() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.popupId(UUID.randomUUID())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.randomUUID())
								.qty(2)
								.unitPrice(1000)
								.build(),
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1200)
								.build()
				))
				.build();

		assertThat(request.isReservationType()).isTrue();
		assertThat(request.isPurchaseType()).isFalse();
		assertThat(request.hasConsistentItemTypes()).isTrue();
		assertThat(request.isValidReservationRequest()).isTrue();
		assertThat(request.isValidPurchaseRequest()).isFalse();
		assertThat(request.getTotalQuantity()).isEqualTo(3);
	}

	@Test
	@DisplayName("구매 타입 검증 - 배송지 필수")
	void purchaseTypeChecksWithAddress() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("PURCHASE")
				.popupId(UUID.randomUUID())
				.address(AddressRequest.builder()
						.address1("123 Main St")
						.build())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("GOODS")
								.goodsVariantId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1500)
								.build()
				))
				.build();

		assertThat(request.isReservationType()).isFalse();
		assertThat(request.isPurchaseType()).isTrue();
		assertThat(request.hasConsistentItemTypes()).isTrue();
		assertThat(request.isValidPurchaseRequest()).isTrue();
	}

	@Test
	@DisplayName("혼합 아이템 타입은 일관성 없음")
	void mixedItemTypesAreNotConsistent() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.popupId(UUID.randomUUID())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1000)
								.build(),
						OrderItemRequest.builder()
								.orderItemType("GOODS")
								.goodsVariantId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1500)
								.build()
				))
				.build();

		assertThat(request.hasConsistentItemTypes()).isFalse();
		assertThat(request.isValidReservationRequest()).isFalse();
	}

	@Test
	@DisplayName("배송지 없는 구매 요청은 유효하지 않음")
	void purchaseRequestWithoutAddressIsInvalid() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("PURCHASE")
				.popupId(UUID.randomUUID())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("GOODS")
								.goodsVariantId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1500)
								.build()
				))
				.build();

		assertThat(request.isValidPurchaseRequest()).isFalse();
	}
}
