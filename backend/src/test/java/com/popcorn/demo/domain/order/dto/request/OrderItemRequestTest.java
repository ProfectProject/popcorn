package com.popcorn.demo.domain.order.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemRequestTest {

	@Test
	@DisplayName("예약 아이템 검증 및 식별자 확인")
	void reservationItemValidationAndIdentifiers() {
		UUID sessionId = UUID.randomUUID();
		OrderItemRequest item = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.sessionId(sessionId)
				.qty(2)
				.unitPrice(1000)
				.build();

		assertThat(item.isReservationType()).isTrue();
		assertThat(item.isMerchType()).isFalse();
		assertThat(item.hasRequiredFields()).isTrue();
		assertThat(item.hasUnnecessaryFields()).isFalse();
		assertThat(item.getSessionOptionKey()).isEqualTo(sessionId.toString());
		assertThat(item.getStockIdentifier()).isEqualTo(sessionId.toString());
		assertThat(item.getDisplayDescription())
				.contains(sessionId.toString())
				.contains("2");
	}

	@Test
	@DisplayName("굿즈 아이템 검증 및 식별자 확인")
	void goodsItemValidationAndIdentifiers() {
		UUID goodsVariantId = UUID.randomUUID();

		OrderItemRequest item = OrderItemRequest.builder()
				.orderItemType("GOODS")
				.goodsVariantId(goodsVariantId)
				.qty(1)
				.unitPrice(1500)
				.build();

		assertThat(item.isReservationType()).isFalse();
		assertThat(item.isGoodsType()).isTrue();
		assertThat(item.hasRequiredFields()).isTrue();
		assertThat(item.hasUnnecessaryFields()).isFalse();
		assertThat(item.getSessionOptionKey()).isNull();
		assertThat(item.getStockIdentifier()).isEqualTo("VARIANT_" + goodsVariantId);
		assertThat(item.getDisplayDescription()).contains(goodsVariantId.toString()).contains("1");
	}

	@Test
	@DisplayName("필수 값 누락 시 검증 실패")
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
	@DisplayName("불필요한 필드 감지")
	void unnecessaryFieldsAreDetected() {
		UUID sessionId = UUID.randomUUID();
		UUID goodsVariantId = UUID.randomUUID();

		OrderItemRequest reservationWithMerch = OrderItemRequest.builder()
				.orderItemType("RESERVATION")
				.sessionId(sessionId)
				.goodsVariantId(goodsVariantId)
				.qty(1)
				.unitPrice(1000)
				.build();

		OrderItemRequest merchWithReservationFields = OrderItemRequest.builder()
				.orderItemType("GOODS")
				.sessionId(sessionId)
				.goodsVariantId(goodsVariantId)
				.qty(1)
				.unitPrice(1500)
				.build();

		assertThat(reservationWithMerch.hasUnnecessaryFields()).isTrue();
		assertThat(merchWithReservationFields.hasUnnecessaryFields()).isTrue();
	}
}
