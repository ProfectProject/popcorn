package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupScheduleListResponse;
import com.popcorn.demo.domain.popup.repository.PopupScheduleQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupScheduleView;

class PopupScheduleQueryServiceTest {

	@Test
	@DisplayName("회차 조회 - 필드 매핑")
	void getProductSessions_mapsFields() {
		PopupScheduleQueryRepository repository = Mockito.mock(PopupScheduleQueryRepository.class);
		PopupScheduleQueryService service = new PopupScheduleQueryService(repository);

		PopupScheduleView view = new TestScheduleView(
				"00000000-0000-0000-0000-000000000201",
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0),
				12000,
				50,
				50,
				true
		);

		when(repository.findProductSessions(
				eq(UUID.fromString("00000000-0000-0000-0000-000000000101")),
				eq(LocalDateTime.of(2025, 1, 1, 0, 0)),
				eq(LocalDateTime.of(2025, 1, 31, 23, 59))))
				.thenReturn(List.of(view));

		PopupScheduleListResponse response = service.getProductSessions(PopupScheduleListQuery.builder()
				.popupId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.from(LocalDateTime.of(2025, 1, 1, 0, 0))
				.to(LocalDateTime.of(2025, 1, 31, 23, 59))
				.build());

		assertEquals(1, response.getItems().size());
		assertEquals(12000, response.getItems().get(0).getPrice());
		assertEquals(50, response.getItems().get(0).getCapacity());
		assertEquals(50, response.getItems().get(0).getRemainingCapacity());
		assertEquals(true, response.getItems().get(0).getIsActive());
	}

	@Test
	@DisplayName("회차 조회 - 비활성 정보 매핑")
	void getProductSessions_mapsInactive() {
		PopupScheduleQueryRepository repository = Mockito.mock(PopupScheduleQueryRepository.class);
		PopupScheduleQueryService service = new PopupScheduleQueryService(repository);

		PopupScheduleView view = new TestScheduleView(
				"00000000-0000-0000-0000-000000000202",
				LocalDateTime.of(2025, 1, 10, 10, 0),
				LocalDateTime.of(2025, 1, 10, 18, 0),
				8000,
				30,
				0,
				false
		);

		when(repository.findProductSessions(
				eq(UUID.fromString("00000000-0000-0000-0000-000000000101")),
				eq(null),
				eq(null)))
				.thenReturn(List.of(view));

		PopupScheduleListResponse response = service.getProductSessions(PopupScheduleListQuery.builder()
				.popupId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build());

		assertEquals(1, response.getItems().size());
		assertEquals(false, response.getItems().get(0).getIsActive());
	}

	private static class TestScheduleView implements PopupScheduleView {
		private final String id;
		private final LocalDateTime startAt;
		private final LocalDateTime endAt;
		private final Integer price;
		private final Integer capacity;
		private final Integer remainingCapacity;
		private final Boolean isActive;

		private TestScheduleView(String id, LocalDateTime startAt, LocalDateTime endAt,
								 Integer price, Integer capacity, Integer remainingCapacity, Boolean isActive) {
			this.id = id;
			this.startAt = startAt;
			this.endAt = endAt;
			this.price = price;
			this.capacity = capacity;
			this.remainingCapacity = remainingCapacity;
			this.isActive = isActive;
		}

    }
}
