package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupScheduleListResponse;
import com.popcorn.demo.domain.popup.event.PopupSearchEvent;
import com.popcorn.demo.domain.popup.event.PopupViewedEvent;

class PopupServiceTest {

	@Test
	@DisplayName("팝업 목록 조회 - 검증/정규화 후 이벤트 발행")
	void getPopups_publishesSearchEvent() {
		PopupQueryService queryService = Mockito.mock(PopupQueryService.class);
		PopupScheduleQueryService sessionQueryService = Mockito.mock(PopupScheduleQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupService service = new PopupService(
				queryService, sessionQueryService, validationService, publisher);

		PopupListQuery request = PopupListQuery.builder()
				.category("FOOD")
				.page(1)
				.size(20)
				.build();
		PopupListQuery normalized = PopupListQuery.builder()
				.category("FOOD")
				.page(1)
				.size(20)
				.build();
		PopupListResponse response = PopupListResponse.builder()
				.page(1)
				.size(20)
				.total(3)
				.items(java.util.List.of())
				.build();

		when(validationService.normalizeListQuery(request)).thenReturn(normalized);
		when(queryService.getPopups(normalized)).thenReturn(response);

		PopupListResponse result = service.getPopups(request);

		assertEquals(3, result.getTotal());
		ArgumentCaptor<PopupSearchEvent> captor = ArgumentCaptor.forClass(PopupSearchEvent.class);
		verify(publisher).publishEvent(captor.capture());
		assertEquals(3, captor.getValue().getTotal());
	}

	@Test
	@DisplayName("팝업 상세 조회 - 이벤트 발행")
	void getPopupDetail_publishesViewedEvent() {
		PopupQueryService queryService = Mockito.mock(PopupQueryService.class);
		PopupScheduleQueryService sessionQueryService = Mockito.mock(PopupScheduleQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupService service = new PopupService(
				queryService, sessionQueryService, validationService, publisher);

		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		PopupDetailQuery query = PopupDetailQuery.of(productId);
		PopupDetailResponse response = PopupDetailResponse.builder()
				.id(productId)
				.storeId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
				.category("FOOD")
				.build();

		when(queryService.getPopupDetail(query)).thenReturn(response);

		PopupDetailResponse result = service.getPopupDetail(query);

		assertEquals(productId, result.getId());
		ArgumentCaptor<PopupViewedEvent> captor = ArgumentCaptor.forClass(PopupViewedEvent.class);
		verify(publisher).publishEvent(captor.capture());
		assertEquals(productId, captor.getValue().getPopupId());
	}

	@Test
	@DisplayName("회차 조회 - 검증 후 조회 서비스 호출")
	void getProductSessions_validatesQuery() {
		PopupQueryService queryService = Mockito.mock(PopupQueryService.class);
		PopupScheduleQueryService sessionQueryService = Mockito.mock(PopupScheduleQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupService service = new PopupService(
				queryService, sessionQueryService, validationService, publisher);

		PopupScheduleListQuery query = PopupScheduleListQuery.builder()
				.popupId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build();
		PopupScheduleListResponse response = PopupScheduleListResponse.builder()
				.items(java.util.List.of())
				.build();

		when(validationService.normalizeSessionQuery(query)).thenReturn(query);
		when(sessionQueryService.getProductSessions(query)).thenReturn(response);

		PopupScheduleListResponse result = service.getProductSessions(query);

		assertEquals(0, result.getItems().size());
		verify(validationService).normalizeSessionQuery(query);
		verify(sessionQueryService).getProductSessions(query);
	}
}
