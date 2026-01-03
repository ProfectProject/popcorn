package com.popcorn.demo.domain.popup.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.query.PopupOptionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupOptionListResponse;
import com.popcorn.demo.domain.popup.repository.PopupOptionQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupOptionView;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupOptionQueryService {

	private final PopupOptionQueryRepository popupOptionQueryRepository;

	@Cacheable(value = "popupOptions", key = "#query.productId")
	public PopupOptionListResponse getProductOptions(PopupOptionListQuery query) {
		List<PopupOptionView> views = popupOptionQueryRepository.findProductOptions(query.getProductId());

		List<PopupOptionListResponse.ItemDto> items = views.stream()
				.map(view -> PopupOptionListResponse.ItemDto.builder()
						.id(toUuid(view.getId()))
						.name(view.getName())
						.price(view.getPrice())
						.capacity(view.getCapacity())
						.isHidden(Boolean.TRUE.equals(view.getIsHidden()))
						.build())
				.toList();

		return PopupOptionListResponse.builder()
				.items(items)
				.build();
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}
}
