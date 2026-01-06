package com.popcorn.demo.domain.popup.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.repository.PopupQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupListView;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupQueryService {

	private final PopupQueryRepository popupQueryRepository;

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	@Cacheable(
			value = "popupList",
			key = "#query.regionId + '_' + #query.category + '_' + #query.keyword + '_' + #query.storeId + '_' + #query.page + '_' + #query.size + '_' + #query.withTotal"
	)
	public PopupListResponse getPopups(PopupListQuery query) {
		Long regionId = query.getRegionId();
		String category = query.getCategory();
		String keyword = query.getKeyword();
		UUID storeId = query.getStoreId();
		Integer page = query.getPage();
		Integer size = query.getSize();
		boolean withTotal = query.getWithTotal() == null || query.getWithTotal();

		int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
		int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		long offset = (long) (normalizedPage - 1) * normalizedSize;

		long total = withTotal
				? popupQueryRepository.countPopups(regionId, category, keyword, storeId)
				: -1L;
		List<PopupListView> views = popupQueryRepository.findPopups(
				regionId, category, keyword, storeId, normalizedSize, offset);

		List<PopupListResponse.ItemDto> items = views.stream()
				.map(view -> PopupListResponse.ItemDto.builder()
						.id(toUuid(view.getId()))
						.storeId(toUuid(view.getStoreId()))
						.title(view.getTitle())
						.category(view.getCategory())
						.status(view.getStatus())
						.eventStartAt(view.getEventStartAt())
						.eventEndAt(view.getEventEndAt())
						.build())
				.toList();

		return PopupListResponse.builder()
				.items(items)
				.page(normalizedPage)
				.size(normalizedSize)
				.total(total)
				.build();
	}

	@Cacheable(value = "popupDetail", key = "#query.popupId")
	public PopupDetailResponse getPopupDetail(PopupDetailQuery query) {
		PopupListView view = popupQueryRepository.findPopupDetail(query.getPopupId())
				.orElseThrow(com.popcorn.demo.domain.popup.exception.PopupException::popupNotFound);

		return PopupDetailResponse.builder()
				.id(toUuid(view.getId()))
				.storeId(toUuid(view.getStoreId()))
				.title(view.getTitle())
				.description(view.getDescription())
				.category(view.getCategory())
				.status(view.getStatus())
				.eventStartAt(view.getEventStartAt())
				.eventEndAt(view.getEventEndAt())
				.build();
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}
}
