package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
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
				"예약형 팝업",
				"FOOD",
				"OPEN",
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0)
		);

		when(repository.countPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null)))
				.thenReturn(1L);
		when(repository.findPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null), eq(100), eq(0L)))
				.thenReturn(List.of(view));

		PopupListQuery query = PopupListQuery.builder()
				.regionId(101L)
				.category(PopupCategory.FOOD)
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
		assertEquals(PopupStatus.OPEN, response.getItems().get(0).getStatus());
		verify(repository).countPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null));
		verify(repository).findPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null), eq(100), eq(0L));
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
				"예약형 팝업",
				"FOOD",
				"OPEN",
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0)
		);

		when(repository.findPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null), eq(20), eq(0L)))
				.thenReturn(List.of(view));

		PopupListQuery query = PopupListQuery.builder()
				.regionId(101L)
				.category(PopupCategory.FOOD)
				.keyword("팝업")
				.page(1)
				.size(20)
				.withTotal(false)
				.build();

		PopupListResponse response = service.getPopups(query);

		assertEquals(-1L, response.getTotal());
		verify(repository, Mockito.never()).countPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null));
		verify(repository).findPopups(eq(101L), eq("FOOD"), eq("팝업"), eq(null), eq(20), eq(0L));
	}

	@Test
	@DisplayName("팝업 목록 조회 - 일정 정보가 없으면 null 반환")
	void getPopups_returnsNullScheduleWhenEmpty() {
		PopupQueryRepository repository = Mockito.mock(PopupQueryRepository.class);
		PopupQueryService service = new PopupQueryService(repository);

		PopupListView view = new TestPopupView(
				"00000000-0000-0000-0000-000000000101",
				"00000000-0000-0000-0000-000000000001",
				"Seed Popup 1",
				null,
				"FOOD",
				"OPEN",
				null,
				null
		);

		when(repository.countPopups(eq(null), eq(null), eq(null), eq(null)))
				.thenReturn(1L);
		when(repository.findPopups(eq(null), eq(null), eq(null), eq(null), eq(20), eq(0L)))
				.thenReturn(List.of(view));

		PopupListResponse response = service.getPopups(PopupListQuery.builder().build());

		assertEquals(1, response.getItems().size());
		assertNull(response.getItems().get(0).getEventStartAt());
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
				"예약형 팝업",
				"FOOD",
				"OPEN",
				LocalDateTime.of(2025, 1, 1, 10, 0),
				LocalDateTime.of(2025, 1, 5, 18, 0)
		);
		when(repository.findPopupDetail(productId)).thenReturn(Optional.of(view));

		PopupDetailResponse response = service.getPopupDetail(PopupDetailQuery.of(productId));

		assertEquals(productId, response.getId());
		assertEquals("Seed Popup 1", response.getTitle());
		assertEquals("예약형 팝업", response.getDescription());
	}

	private record TestPopupView(String id, String storeId, String title, String description, String category,
								 String status, LocalDateTime eventStartAt,
								 LocalDateTime eventEndAt) implements PopupListView {


	}
}
