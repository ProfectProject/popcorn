package com.popcorn.demo.domain.popup.service;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupScheduleListResponse;
import com.popcorn.demo.domain.popup.event.PopupSearchEvent;
import com.popcorn.demo.domain.popup.event.PopupViewedEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupService {

	private final PopupQueryService popupQueryService;
	private final PopupScheduleQueryService popupScheduleQueryService;
	private final PopupValidationService popupValidationService;
	private final ApplicationEventPublisher eventPublisher;

	public PopupListResponse getPopups(PopupListQuery query) {
		PopupListQuery normalizedQuery = popupValidationService.normalizeListQuery(query);
		PopupListResponse response = popupQueryService.getPopups(normalizedQuery);

		eventPublisher.publishEvent(PopupSearchEvent.builder()
				.regionId(normalizedQuery.getRegionId())
				.category(normalizedQuery.getCategory())
				.keyword(normalizedQuery.getKeyword())
				.storeId(normalizedQuery.getStoreId())
				.page(normalizedQuery.getPage())
				.size(normalizedQuery.getSize())
				.total(response.getTotal())
				.occurredAt(LocalDateTime.now())
				.build());

		return response;
	}

	public PopupDetailResponse getPopupDetail(PopupDetailQuery query) {
		popupValidationService.validateDetailQuery(query);
		PopupDetailResponse response = popupQueryService.getPopupDetail(query);

		eventPublisher.publishEvent(PopupViewedEvent.builder()
				.popupId(response.getId())
				.storeId(response.getStoreId())
				.category(response.getCategory())
				.occurredAt(LocalDateTime.now())
				.build());

		return response;
	}

	public PopupScheduleListResponse getProductSessions(PopupScheduleListQuery query) {
		PopupScheduleListQuery normalizedQuery = popupValidationService.normalizeSessionQuery(query);
		return popupScheduleQueryService.getProductSessions(normalizedQuery);
	}
}
