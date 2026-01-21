package com.popcorn.store.domain.goods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.store.domain.goods.dto.GoodsListResponse;
import com.popcorn.store.domain.goods.dto.GoodsResponseCode;
import com.popcorn.store.domain.goods.dto.GoodsStockResponse;
import com.popcorn.store.domain.goods.exception.GoodsException;
import com.popcorn.store.domain.goods.repository.GoodsReservationRepository;
import com.popcorn.store.domain.goods.repository.GoodsVariantRepository;

class GoodsServiceTest {

	@Test
	@DisplayName("굿즈 목록 조회 - 사용자용 목록 반환")
	void listForUserReturnsItems() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		UUID popupId = UUID.randomUUID();
		when(variantRepository.findAllByPopupIdAndIsActiveTrueAndDeletedAtIsNullOrderByCreatedAtDesc(popupId))
				.thenReturn(List.of());

		GoodsListResponse response = service.listForUser(popupId);

		assertThat(response.getItems()).isEmpty();
	}

	@Test
	@DisplayName("굿즈 예약 - 수량이 0 이하이면 실패")
	void reservationGoodsRejectsInvalidQuantity() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		assertThatThrownBy(() -> service.reservationGoods(UUID.randomUUID(), UUID.randomUUID(), 0))
				.isInstanceOf(GoodsException.class)
				.extracting("responseCode")
				.isEqualTo(GoodsResponseCode.INVALID_QUANTITY);
	}

	@Test
	@DisplayName("굿즈 예약 - 재고 부족이면 실패")
	void reservationGoodsFailsWhenInsufficient() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		UUID goodsId = UUID.randomUUID();
		when(reservationRepository.reserveStock(eq(goodsId), eq(2))).thenReturn(null);

		assertThatThrownBy(() -> service.reservationGoods(UUID.randomUUID(), goodsId, 2))
				.isInstanceOf(GoodsException.class)
				.extracting("responseCode")
				.isEqualTo(GoodsResponseCode.INSUFFICIENT_STOCK);
	}

	@Test
	@DisplayName("굿즈 예약 - 성공 시 결과 반환")
	void reservationGoodsReturnsResponse() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		UUID goodsId = UUID.randomUUID();
		GoodsStockResponse expected = GoodsStockResponse.builder()
				.goodsId(goodsId)
				.stock(10)
				.reservationStock(2)
				.build();
		when(reservationRepository.reserveStock(eq(goodsId), eq(2))).thenReturn(expected);

		GoodsStockResponse response = service.reservationGoods(UUID.randomUUID(), goodsId, 2);

		assertThat(response.getGoodsId()).isEqualTo(goodsId);
		assertThat(response.getStock()).isEqualTo(10);
		assertThat(response.getReservationStock()).isEqualTo(2);
	}

	@Test
	@DisplayName("굿즈 예약 취소 - 수량이 0 이하이면 실패")
	void cancelReservationRejectsInvalidQuantity() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		assertThatThrownBy(() -> service.cancelReservationGoods(UUID.randomUUID(), UUID.randomUUID(), -1))
				.isInstanceOf(GoodsException.class)
				.extracting("responseCode")
				.isEqualTo(GoodsResponseCode.INVALID_QUANTITY);
	}

	@Test
	@DisplayName("굿즈 예약 실패 처리 - 성공 시 결과 반환")
	void failReservationReturnsResponse() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		UUID goodsId = UUID.randomUUID();
		GoodsStockResponse expected = GoodsStockResponse.builder()
				.goodsId(goodsId)
				.stock(8)
				.reservationStock(1)
				.build();
		when(reservationRepository.failStock(eq(goodsId), eq(1))).thenReturn(expected);

		GoodsStockResponse response = service.failReservationGoods(UUID.randomUUID(), goodsId, 1);

		assertThat(response.getGoodsId()).isEqualTo(goodsId);
		assertThat(response.getStock()).isEqualTo(8);
	}

	@Test
	@DisplayName("굿즈 예약 완료 처리 - 재고 부족이면 실패")
	void completeReservationFailsWhenInsufficient() {
		GoodsVariantRepository variantRepository = Mockito.mock(GoodsVariantRepository.class);
		GoodsReservationRepository reservationRepository = Mockito.mock(GoodsReservationRepository.class);
		GoodsService service = new GoodsService(variantRepository, reservationRepository);

		UUID goodsId = UUID.randomUUID();
		when(reservationRepository.completeStock(eq(goodsId), eq(2))).thenReturn(null);

		assertThatThrownBy(() -> service.completeReservationGoods(UUID.randomUUID(), goodsId, 2))
				.isInstanceOf(GoodsException.class)
				.extracting("responseCode")
				.isEqualTo(GoodsResponseCode.INSUFFICIENT_STOCK);
	}
}
