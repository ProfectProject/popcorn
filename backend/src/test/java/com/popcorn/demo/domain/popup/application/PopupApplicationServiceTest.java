package com.popcorn.demo.domain.popup.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.popcorn.demo.domain.popup.dto.query.PopupOptionListQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupSessionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupOptionListResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse;
import com.popcorn.demo.domain.popup.event.PopupSearchEvent;
import com.popcorn.demo.domain.popup.event.PopupViewedEvent;
import com.popcorn.demo.domain.popup.service.PopupOptionQueryService;
import com.popcorn.demo.domain.popup.service.PopupQueryService;
import com.popcorn.demo.domain.popup.service.PopupSessionQueryService;
import com.popcorn.demo.domain.popup.service.PopupValidationService;

class PopupApplicationServiceTest {

	@Test
	@DisplayName("팝업 목록 조회 - 검증/정규화 후 이벤트 발행")
	void getPopups_publishesSearchEvent() {
		PopupQueryService queryService = Mockito.mock(PopupQueryService.class);
		PopupSessionQueryService sessionQueryService = Mockito.mock(PopupSessionQueryService.class);
		PopupOptionQueryService optionQueryService = Mockito.mock(PopupOptionQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupApplicationService service = new PopupApplicationService(
				queryService, sessionQueryService, optionQueryService, validationService, publisher);

		PopupListQuery request = PopupListQuery.builder()
				.category("POPUP")
				.page(1)
				.size(20)
				.build();
		PopupListQuery normalized = PopupListQuery.builder()
				.category("POPUP")
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
		PopupSessionQueryService sessionQueryService = Mockito.mock(PopupSessionQueryService.class);
		PopupOptionQueryService optionQueryService = Mockito.mock(PopupOptionQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupApplicationService service = new PopupApplicationService(
				queryService, sessionQueryService, optionQueryService, validationService, publisher);

		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		PopupDetailQuery query = PopupDetailQuery.of(productId);
		PopupDetailResponse response = PopupDetailResponse.builder()
				.id(productId)
				.storeId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
				.category("POPUP")
				.build();

		when(queryService.getPopupDetail(query)).thenReturn(response);

		PopupDetailResponse result = service.getPopupDetail(query);

		assertEquals(productId, result.getId());
		ArgumentCaptor<PopupViewedEvent> captor = ArgumentCaptor.forClass(PopupViewedEvent.class);
		verify(publisher).publishEvent(captor.capture());
		assertEquals(productId, captor.getValue().getProductId());
	}

	@Test
	@DisplayName("회차 조회 - 검증 후 조회 서비스 호출")
	void getProductSessions_validatesQuery() {
		PopupQueryService queryService = Mockito.mock(PopupQueryService.class);
		PopupSessionQueryService sessionQueryService = Mockito.mock(PopupSessionQueryService.class);
		PopupOptionQueryService optionQueryService = Mockito.mock(PopupOptionQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupApplicationService service = new PopupApplicationService(
				queryService, sessionQueryService, optionQueryService, validationService, publisher);

		PopupSessionListQuery query = PopupSessionListQuery.builder()
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build();
		PopupSessionListResponse response = PopupSessionListResponse.builder()
				.items(java.util.List.of())
				.build();

		when(validationService.normalizeSessionQuery(query)).thenReturn(query);
		when(sessionQueryService.getProductSessions(query)).thenReturn(response);

		PopupSessionListResponse result = service.getProductSessions(query);

		assertEquals(0, result.getItems().size());
		verify(validationService).normalizeSessionQuery(query);
		verify(sessionQueryService).getProductSessions(query);
	}

	@Test
	@DisplayName("옵션 조회 - 검증 후 조회 서비스 호출")
	void getProductOptions_validatesQuery() {
		PopupQueryService queryService = Mockito.mock(PopupQueryService.class);
		PopupSessionQueryService sessionQueryService = Mockito.mock(PopupSessionQueryService.class);
		PopupOptionQueryService optionQueryService = Mockito.mock(PopupOptionQueryService.class);
		PopupValidationService validationService = Mockito.mock(PopupValidationService.class);
		ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
		PopupApplicationService service = new PopupApplicationService(
				queryService, sessionQueryService, optionQueryService, validationService, publisher);

		PopupOptionListQuery query = PopupOptionListQuery.builder()
				.productId(UUID.fromString("00000000-0000-0000-0000-000000000101"))
				.build();
		PopupOptionListResponse response = PopupOptionListResponse.builder()
				.items(java.util.List.of())
				.build();

		when(validationService.normalizeOptionQuery(query)).thenReturn(query);
		when(optionQueryService.getProductOptions(query)).thenReturn(response);

		PopupOptionListResponse result = service.getProductOptions(query);

		assertEquals(0, result.getItems().size());
		verify(validationService).normalizeOptionQuery(query);
		verify(optionQueryService).getProductOptions(query);
	}
}
