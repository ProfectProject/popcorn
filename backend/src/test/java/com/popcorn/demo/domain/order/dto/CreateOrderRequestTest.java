package com.popcorn.demo.domain.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateOrderRequestTest {

	@Test
	@DisplayName("Reservation type checks and total quantity")
	void reservationTypeChecksAndTotalQuantity() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.randomUUID())
								.optionId(UUID.randomUUID())
								.qty(2)
								.unitPrice(1000)
								.build(),
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.randomUUID())
								.optionId(UUID.randomUUID())
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
	@DisplayName("Purchase type checks with address requirement")
	void purchaseTypeChecksWithAddress() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("PURCHASE")
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.address(AddressRequest.builder()
						.address1("123 Main St")
						.build())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("MERCH")
								.merchVariantId(UUID.randomUUID())
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
	@DisplayName("Mixed item types are not consistent")
	void mixedItemTypesAreNotConsistent() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("RESERVATION")
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("RESERVATION")
								.sessionId(UUID.randomUUID())
								.optionId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1000)
								.build(),
						OrderItemRequest.builder()
								.orderItemType("MERCH")
								.merchVariantId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1500)
								.build()
				))
				.build();

		assertThat(request.hasConsistentItemTypes()).isFalse();
		assertThat(request.isValidReservationRequest()).isFalse();
	}

	@Test
	@DisplayName("Purchase request without address is invalid")
	void purchaseRequestWithoutAddressIsInvalid() {
		CreateOrderRequest request = CreateOrderRequest.builder()
				.orderType("PURCHASE")
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.items(List.of(
						OrderItemRequest.builder()
								.orderItemType("MERCH")
								.merchVariantId(UUID.randomUUID())
								.qty(1)
								.unitPrice(1500)
								.build()
				))
				.build();

		assertThat(request.isValidPurchaseRequest()).isFalse();
	}
}
