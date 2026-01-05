package com.popcorn.demo.domain.popup.application;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupApplicationService {

	private final PopupQueryService popupQueryService;
	private final PopupSessionQueryService popupSessionQueryService;
	private final PopupOptionQueryService popupOptionQueryService;
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
				.productId(response.getId())
				.storeId(response.getStoreId())
				.category(response.getCategory())
				.regionId(response.getRegionId())
				.occurredAt(LocalDateTime.now())
				.build());

		return response;
	}

	public PopupSessionListResponse getProductSessions(PopupSessionListQuery query) {
		PopupSessionListQuery normalizedQuery = popupValidationService.normalizeSessionQuery(query);
		return popupSessionQueryService.getProductSessions(normalizedQuery);
	}

	public PopupOptionListResponse getProductOptions(PopupOptionListQuery query) {
		PopupOptionListQuery normalizedQuery = popupValidationService.normalizeOptionQuery(query);
		return popupOptionQueryService.getProductOptions(normalizedQuery);
	}
}
