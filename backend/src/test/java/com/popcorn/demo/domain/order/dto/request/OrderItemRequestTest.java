package com.popcorn.demo.domain.order.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemRequestTest {

	@Test
	@DisplayName("Reservation item validation and identifiers")
	void reservationItemValidationAndIdentifiers() {
		UUID sessionId = UUID.randomUUID();
		UUID optionId = UUID.randomUUID();

		OrderItemRequest item = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.sessionId(sessionId)
				.optionId(optionId)
				.qty(2)
				.unitPrice(1000)
				.build();

		assertThat(item.isReservationType()).isTrue();
		assertThat(item.isMerchType()).isFalse();
		assertThat(item.hasRequiredFields()).isTrue();
		assertThat(item.hasUnnecessaryFields()).isFalse();
		assertThat(item.getSessionOptionKey()).isEqualTo(sessionId + "-" + optionId);
		assertThat(item.getStockIdentifier()).isEqualTo(sessionId + "-" + optionId);
		assertThat(item.getDisplayDescription())
				.contains(sessionId.toString())
				.contains(optionId.toString())
				.contains("2");
	}

	@Test
	@DisplayName("Merch item validation and identifiers")
	void merchItemValidationAndIdentifiers() {
		UUID merchVariantId = UUID.randomUUID();

		OrderItemRequest item = OrderItemRequest.builder()
				.orderItemType("MERCH")
				.merchVariantId(merchVariantId)
				.qty(1)
				.unitPrice(1500)
				.build();

		assertThat(item.isReservationType()).isFalse();
		assertThat(item.isMerchType()).isTrue();
		assertThat(item.hasRequiredFields()).isTrue();
		assertThat(item.hasUnnecessaryFields()).isFalse();
		assertThat(item.getSessionOptionKey()).isNull();
		assertThat(item.getStockIdentifier()).isEqualTo("VARIANT_" + merchVariantId);
		assertThat(item.getDisplayDescription()).contains(merchVariantId.toString()).contains("1");
	}

	@Test
	@DisplayName("Missing required fields fail validation")
	void missingRequiredFieldsFailValidation() {
		OrderItemRequest reservationItem = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.qty(1)
				.unitPrice(1000)
				.build();

		OrderItemRequest merchItem = OrderItemRequest.builder()
				.orderItemType("MERCH")
				.qty(1)
				.unitPrice(1500)
				.build();

		assertThat(reservationItem.hasRequiredFields()).isFalse();
		assertThat(merchItem.hasRequiredFields()).isFalse();
	}

	@Test
	@DisplayName("Unnecessary fields are detected")
	void unnecessaryFieldsAreDetected() {
		UUID sessionId = UUID.randomUUID();
		UUID optionId = UUID.randomUUID();
		UUID merchVariantId = UUID.randomUUID();

		OrderItemRequest reservationWithMerch = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.sessionId(sessionId)
				.optionId(optionId)
				.merchVariantId(merchVariantId)
				.qty(1)
				.unitPrice(1000)
				.build();

		OrderItemRequest merchWithReservationFields = OrderItemRequest.builder()
				.orderItemType("MERCH")
				.sessionId(sessionId)
				.optionId(optionId)
				.merchVariantId(merchVariantId)
				.qty(1)
				.unitPrice(1500)
				.build();

		assertThat(reservationWithMerch.hasUnnecessaryFields()).isTrue();
		assertThat(merchWithReservationFields.hasUnnecessaryFields()).isTrue();
	}
}
