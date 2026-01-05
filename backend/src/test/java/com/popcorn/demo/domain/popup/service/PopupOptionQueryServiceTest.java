package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.popup.dto.query.PopupOptionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupOptionListResponse;
import com.popcorn.demo.domain.popup.repository.PopupOptionQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupOptionView;

class PopupOptionQueryServiceTest {

	@Test
	@DisplayName("옵션 조회 - 필드 매핑")
	void getProductOptions_mapsFields() {
		PopupOptionQueryRepository repository = Mockito.mock(PopupOptionQueryRepository.class);
		PopupOptionQueryService service = new PopupOptionQueryService(repository);

		PopupOptionView view = new TestOptionView(
				"00000000-0000-0000-0000-000000000301",
				"일반 좌석",
				10000,
				20,
				false
		);

		when(repository.findProductOptions(eq(UUID.fromString("00000000-0000-0000-0000-000000000101"))))
				.thenReturn(List.of(view));

		PopupOptionListResponse response = service.getProductOptions(PopupOptionListQuery.builder()
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build());

		assertEquals(1, response.getItems().size());
		assertEquals("일반 좌석", response.getItems().get(0).getName());
		assertEquals(10000, response.getItems().get(0).getPrice());
		assertEquals(20, response.getItems().get(0).getCapacity());
	}

	private static class TestOptionView implements PopupOptionView {
		private final String id;
		private final String name;
		private final Integer price;
		private final Integer capacity;
		private final Boolean isHidden;

		private TestOptionView(String id, String name, Integer price, Integer capacity, Boolean isHidden) {
			this.id = id;
			this.name = name;
			this.price = price;
			this.capacity = capacity;
			this.isHidden = isHidden;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Integer getPrice() {
			return price;
		}

		@Override
		public Integer getCapacity() {
			return capacity;
		}

		@Override
		public Boolean getIsHidden() {
			return isHidden;
		}
	}
}
