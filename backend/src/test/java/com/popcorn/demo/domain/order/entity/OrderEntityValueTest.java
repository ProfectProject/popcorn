package com.popcorn.demo.domain.order.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderEntityValueTest {

	@Test
	@DisplayName("Goods detail factory and helpers")
	void goodsDetailFactoryAndHelpers() {
		UUID variantId = UUID.randomUUID();
		GoodsDetail detail = GoodsDetail.of(variantId, "SKU-1", "Item", "Blue");

		assertThat(detail.isValid()).isTrue();
		assertThat(detail.isSameVariant(variantId)).isTrue();
		assertThat(detail.hasSku("SKU-1")).isTrue();
		assertThat(detail.getDisplayName()).isEqualTo("Item - Blue");
		assertThat(detail.getStockIdentifier()).isEqualTo("SKU-1");

		GoodsDetail minimal = GoodsDetail.of(variantId);
		assertThat(minimal.getStockIdentifier()).isEqualTo("VARIANT_" + variantId);
	}

	@Test
	@DisplayName("Goods detail requires variant id")
	void goodsDetailRequiresVariantId() {
		assertThatThrownBy(() -> GoodsDetail.of(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Reservation detail factory and helpers")
	void reservationDetailFactoryAndHelpers() {
		UUID sessionOptionId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		UUID optionId = UUID.randomUUID();
		ReservationDetail detail = ReservationDetail.of(sessionOptionId, sessionId, optionId);

		assertThat(detail.isValid()).isTrue();
		assertThat(detail.belongsToSession(sessionId)).isTrue();
		assertThat(detail.hasOption(optionId)).isTrue();
	}

	@Test
	@DisplayName("Reservation detail requires session option id")
	void reservationDetailRequiresSessionOptionId() {
		assertThatThrownBy(() -> ReservationDetail.of(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Order item type checks reservation and goods")
	void orderItemTypeChecks() {
		OrderItem reservationItem = OrderItem.builder().orderItemType(OrderItemType.RESERVATION).build();
		OrderItem goodsItem = OrderItem.builder().orderItemType(OrderItemType.GOODS).build();

		assertThat(reservationItem.isReservationType()).isTrue();
		assertThat(reservationItem.isGoodsType()).isFalse();
		assertThat(goodsItem.isReservationType()).isFalse();
		assertThat(goodsItem.isGoodsType()).isTrue();
	}
}
