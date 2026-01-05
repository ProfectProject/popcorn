package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.repository.PopupQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupListView;

class PopupQueryServiceTest {

	@Test
	@DisplayName("팝업 목록 조회 - 필드 매핑 및 페이징")
	void getPopups_mapsFields() {
		PopupQueryRepository repository = Mockito.mock(PopupQueryRepository.class);
		PopupQueryService service = new PopupQueryService(repository);

		PopupListView view = new TestPopupView(
				"00000000-0000-0000-0000-000000000101",
				"00000000-0000-0000-0000-000000000001",
				"Seed Popup 1",
				"RESERVATION",
				"POPUP",
				101L,
				false,
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0),
				"00000000-0000-0000-0000-000000009001",
				"팝업 테스트 장소",
				"서울특별시 강남구 테헤란로 123",
				"ABC빌딩 12층",
				BigDecimal.valueOf(37.498),
				BigDecimal.valueOf(127.027)
		);

		when(repository.countPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null)))
				.thenReturn(1L);
		when(repository.findPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null), eq(100), eq(0L)))
				.thenReturn(List.of(view));

		PopupListQuery query = PopupListQuery.builder()
				.regionId(101L)
				.category("POPUP")
				.keyword("팝업")
				.page(1)
				.size(100)
				.build();

		PopupListResponse response = service.getPopups(query);

		assertEquals(1, response.getItems().size());
		assertEquals(1, response.getPage());
		assertEquals(100, response.getSize());
		assertEquals(1L, response.getTotal());
		assertEquals("Seed Popup 1", response.getItems().get(0).getTitle());
		assertEquals("RESERVATION", response.getItems().get(0).getProductType());
		assertEquals("팝업 테스트 장소", response.getItems().get(0).getLocation().getName());
		verify(repository).countPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null));
		verify(repository).findPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null), eq(100), eq(0L));
	}

	@Test
	@DisplayName("팝업 목록 조회 - 전체 개수 비활성화 시 카운트 생략")
	void getPopups_skipsCountWhenWithTotalFalse() {
		PopupQueryRepository repository = Mockito.mock(PopupQueryRepository.class);
		PopupQueryService service = new PopupQueryService(repository);

		PopupListView view = new TestPopupView(
				"00000000-0000-0000-0000-000000000101",
				"00000000-0000-0000-0000-000000000001",
				"Seed Popup 1",
				"RESERVATION",
				"POPUP",
				101L,
				false,
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0),
				"00000000-0000-0000-0000-000000009001",
				"팝업 테스트 장소",
				"서울특별시 강남구 테헤란로 123",
				"ABC빌딩 12층",
				BigDecimal.valueOf(37.498),
				BigDecimal.valueOf(127.027)
		);

		when(repository.findPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null), eq(20), eq(0L)))
				.thenReturn(List.of(view));

		PopupListQuery query = PopupListQuery.builder()
				.regionId(101L)
				.category("POPUP")
				.keyword("팝업")
				.page(1)
				.size(20)
				.withTotal(false)
				.build();

		PopupListResponse response = service.getPopups(query);

		assertEquals(-1L, response.getTotal());
		verify(repository, Mockito.never()).countPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null));
		verify(repository).findPopups(eq(101L), eq("POPUP"), eq("팝업"), eq(null), eq(20), eq(0L));
	}

	@Test
	@DisplayName("팝업 목록 조회 - 위치 정보가 없으면 null 반환")
	void getPopups_returnsNullLocationWhenEmpty() {
		PopupQueryRepository repository = Mockito.mock(PopupQueryRepository.class);
		PopupQueryService service = new PopupQueryService(repository);

		PopupListView view = new TestPopupView(
				"00000000-0000-0000-0000-000000000101",
				"00000000-0000-0000-0000-000000000001",
				"Seed Popup 1",
				"RESERVATION",
				"POPUP",
				101L,
				false,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		when(repository.countPopups(eq(null), eq(null), eq(null), eq(null)))
				.thenReturn(1L);
		when(repository.findPopups(eq(null), eq(null), eq(null), eq(null), eq(20), eq(0L)))
				.thenReturn(List.of(view));

		PopupListResponse response = service.getPopups(PopupListQuery.builder().build());

		assertEquals(1, response.getItems().size());
		assertNull(response.getItems().get(0).getLocation());
	}

	@Test
	@DisplayName("팝업 상세 조회 - 없으면 예외")
	void getPopupDetail_throwsWhenNotFound() {
		PopupQueryRepository repository = Mockito.mock(PopupQueryRepository.class);
		PopupQueryService service = new PopupQueryService(repository);

		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		when(repository.findPopupDetail(productId)).thenReturn(Optional.empty());

		assertThrows(PopupException.class, () -> service.getPopupDetail(PopupDetailQuery.of(productId)));
	}

	@Test
	@DisplayName("팝업 상세 조회 - 정상 매핑")
	void getPopupDetail_mapsFields() {
		PopupQueryRepository repository = Mockito.mock(PopupQueryRepository.class);
		PopupQueryService service = new PopupQueryService(repository);

		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		PopupListView view = new TestPopupView(
				productId.toString(),
				"00000000-0000-0000-0000-000000000001",
				"Seed Popup 1",
				"RESERVATION",
				"POPUP",
				101L,
				false,
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0),
				"00000000-0000-0000-0000-000000009001",
				"팝업 테스트 장소",
				"서울특별시 강남구 테헤란로 123",
				"ABC빌딩 12층",
				BigDecimal.valueOf(37.498),
				BigDecimal.valueOf(127.027)
		);
		when(repository.findPopupDetail(productId)).thenReturn(Optional.of(view));

		PopupDetailResponse response = service.getPopupDetail(PopupDetailQuery.of(productId));

		assertEquals(productId, response.getId());
		assertEquals("Seed Popup 1", response.getTitle());
		assertEquals("RESERVATION", response.getProductType());
		assertEquals("팝업 테스트 장소", response.getLocation().getName());
	}

	private static class TestPopupView implements PopupListView {
		private final String id;
		private final String storeId;
		private final String title;
		private final String productType;
		private final String category;
		private final Long regionId;
		private final Boolean isHidden;
		private final LocalDateTime eventStartAt;
		private final LocalDateTime eventEndAt;
		private final String locationId;
		private final String locationName;
		private final String locationAddress1;
		private final String locationAddress2;
		private final BigDecimal locationLatitude;
		private final BigDecimal locationLongitude;

		private TestPopupView(String id, String storeId, String title, String productType, String category,
				Long regionId, Boolean isHidden, LocalDateTime eventStartAt, LocalDateTime eventEndAt,
				String locationId, String locationName, String locationAddress1, String locationAddress2,
				BigDecimal locationLatitude, BigDecimal locationLongitude) {
			this.id = id;
			this.storeId = storeId;
			this.title = title;
			this.productType = productType;
			this.category = category;
			this.regionId = regionId;
			this.isHidden = isHidden;
			this.eventStartAt = eventStartAt;
			this.eventEndAt = eventEndAt;
			this.locationId = locationId;
			this.locationName = locationName;
			this.locationAddress1 = locationAddress1;
			this.locationAddress2 = locationAddress2;
			this.locationLatitude = locationLatitude;
			this.locationLongitude = locationLongitude;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getStoreId() {
			return storeId;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String getProductType() {
			return productType;
		}

		@Override
		public String getCategory() {
			return category;
		}

		@Override
		public Long getRegionId() {
			return regionId;
		}

		@Override
		public Boolean getIsHidden() {
			return isHidden;
		}

		@Override
		public LocalDateTime getEventStartAt() {
			return eventStartAt;
		}

		@Override
		public LocalDateTime getEventEndAt() {
			return eventEndAt;
		}

		@Override
		public String getLocationId() {
			return locationId;
		}

		@Override
		public String getLocationName() {
			return locationName;
		}

		@Override
		public String getLocationAddress1() {
			return locationAddress1;
		}

		@Override
		public String getLocationAddress2() {
			return locationAddress2;
		}

		@Override
		public BigDecimal getLocationLatitude() {
			return locationLatitude;
		}

		@Override
		public BigDecimal getLocationLongitude() {
			return locationLongitude;
		}
	}
}
