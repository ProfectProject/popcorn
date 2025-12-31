package com.popcorn.demo.domain.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemRequestTest {

	@Test
	@DisplayName("Reservation item validation and identifiers")
	void reservationItemValidationAndIdentifiers() {
		OrderItemRequest item = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.sessionId(10L)
				.optionId(20L)
				.qty(2)
				.build();

		assertThat(item.isReservationType()).isTrue();
		assertThat(item.isMerchType()).isFalse();
		assertThat(item.hasRequiredFields()).isTrue();
		assertThat(item.hasUnnecessaryFields()).isFalse();
		assertThat(item.getSessionOptionKey()).isEqualTo("10-20");
		assertThat(item.getStockIdentifier()).isEqualTo("10-20");
		assertThat(item.getDisplayDescription()).contains("10").contains("20").contains("2");
	}

	@Test
	@DisplayName("Merch item validation and identifiers")
	void merchItemValidationAndIdentifiers() {
		OrderItemRequest item = OrderItemRequest.builder()
				.orderItemType("MERCH")
				.merchVariantId(100L)
				.qty(1)
				.build();

		assertThat(item.isReservationType()).isFalse();
		assertThat(item.isMerchType()).isTrue();
		assertThat(item.hasRequiredFields()).isTrue();
		assertThat(item.hasUnnecessaryFields()).isFalse();
		assertThat(item.getSessionOptionKey()).isNull();
		assertThat(item.getStockIdentifier()).isEqualTo("VARIANT_100");
		assertThat(item.getDisplayDescription()).contains("100").contains("1");
	}

	@Test
	@DisplayName("Missing required fields fail validation")
	void missingRequiredFieldsFailValidation() {
		OrderItemRequest reservationItem = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.qty(1)
				.build();

		OrderItemRequest merchItem = OrderItemRequest.builder()
				.orderItemType("MERCH")
				.qty(1)
				.build();

		assertThat(reservationItem.hasRequiredFields()).isFalse();
		assertThat(merchItem.hasRequiredFields()).isFalse();
	}

	@Test
	@DisplayName("Unnecessary fields are detected")
	void unnecessaryFieldsAreDetected() {
		OrderItemRequest reservationWithMerch = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.sessionId(10L)
				.optionId(20L)
				.merchVariantId(100L)
				.qty(1)
				.build();

		OrderItemRequest merchWithReservationFields = OrderItemRequest.builder()
				.orderItemType("MERCH")
				.sessionId(10L)
				.optionId(20L)
				.merchVariantId(100L)
				.qty(1)
				.build();

		assertThat(reservationWithMerch.hasUnnecessaryFields()).isTrue();
		assertThat(merchWithReservationFields.hasUnnecessaryFields()).isTrue();
	}
}
