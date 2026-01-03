package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.popup.dto.query.PopupSessionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse;
import com.popcorn.demo.domain.popup.repository.PopupSessionQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupSessionView;

class PopupSessionQueryServiceTest {

	@Test
	@DisplayName("회차 조회 - 상태 매핑 및 위치 정보 포함")
	void getProductSessions_mapsStatusAndLocation() {
		PopupSessionQueryRepository repository = Mockito.mock(PopupSessionQueryRepository.class);
		PopupSessionQueryService service = new PopupSessionQueryService(repository);

		PopupSessionView view = new TestSessionView(
				"00000000-0000-0000-0000-000000000201",
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0),
				"UPCOMING",
				"00000000-0000-0000-0000-000000009001",
				"팝업 테스트 장소",
				"서울특별시 강남구 테헤란로 123",
				"ABC빌딩 12층",
				BigDecimal.valueOf(37.498),
				BigDecimal.valueOf(127.027)
		);

		when(repository.findProductSessions(
				eq(UUID.fromString("00000000-0000-0000-0000-000000000101")),
				eq(LocalDateTime.of(2025, 1, 1, 0, 0)),
				eq(LocalDateTime.of(2025, 1, 31, 23, 59))))
				.thenReturn(List.of(view));

		PopupSessionListResponse response = service.getProductSessions(PopupSessionListQuery.builder()
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.from(LocalDateTime.of(2025, 1, 1, 0, 0))
				.to(LocalDateTime.of(2025, 1, 31, 23, 59))
				.build());

		assertEquals(1, response.getItems().size());
		assertEquals("OPEN", response.getItems().get(0).getStatus());
		assertEquals("팝업 테스트 장소", response.getItems().get(0).getLocation().getName());
	}

	@Test
	@DisplayName("회차 조회 - 위치 정보가 없으면 null 반환")
	void getProductSessions_returnsNullLocationWhenEmpty() {
		PopupSessionQueryRepository repository = Mockito.mock(PopupSessionQueryRepository.class);
		PopupSessionQueryService service = new PopupSessionQueryService(repository);

		PopupSessionView view = new TestSessionView(
				"00000000-0000-0000-0000-000000000202",
				LocalDateTime.of(2025, 1, 10, 10, 0),
				LocalDateTime.of(2025, 1, 10, 18, 0),
				"OPEN",
				null,
				null,
				null,
				null,
				null,
				null
		);

		when(repository.findProductSessions(
				eq(UUID.fromString("00000000-0000-0000-0000-000000000101")),
				eq(null),
				eq(null)))
				.thenReturn(List.of(view));

		PopupSessionListResponse response = service.getProductSessions(PopupSessionListQuery.builder()
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build());

		assertEquals(1, response.getItems().size());
		assertNull(response.getItems().get(0).getLocation());
	}

	@Test
	@DisplayName("회차 조회 - ENDED는 CLOSED로 변환")
	void getProductSessions_mapsEndedToClosed() {
		PopupSessionQueryRepository repository = Mockito.mock(PopupSessionQueryRepository.class);
		PopupSessionQueryService service = new PopupSessionQueryService(repository);

		PopupSessionView view = new TestSessionView(
				"00000000-0000-0000-0000-000000000203",
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 1, 12, 0),
				"ENDED",
				null,
				null,
				null,
				null,
				null,
				null
		);

		when(repository.findProductSessions(
				eq(UUID.fromString("00000000-0000-0000-0000-000000000101")),
				eq(null),
				eq(null)))
				.thenReturn(List.of(view));

		PopupSessionListResponse response = service.getProductSessions(PopupSessionListQuery.builder()
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build());

		assertEquals("CLOSED", response.getItems().get(0).getStatus());
	}

	private static class TestSessionView implements PopupSessionView {
		private final String id;
		private final LocalDateTime startAt;
		private final LocalDateTime endAt;
		private final String status;
		private final String locationId;
		private final String locationName;
		private final String locationAddress1;
		private final String locationAddress2;
		private final BigDecimal locationLatitude;
		private final BigDecimal locationLongitude;

		private TestSessionView(String id, LocalDateTime startAt, LocalDateTime endAt, String status,
				String locationId, String locationName, String locationAddress1, String locationAddress2,
				BigDecimal locationLatitude, BigDecimal locationLongitude) {
			this.id = id;
			this.startAt = startAt;
			this.endAt = endAt;
			this.status = status;
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
		public LocalDateTime getStartAt() {
			return startAt;
		}

		@Override
		public LocalDateTime getEndAt() {
			return endAt;
		}

		@Override
		public String getStatus() {
			return status;
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
