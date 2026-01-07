package com.popcorn.demo.domain.popup.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.query.PopupSessionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse;
import com.popcorn.demo.domain.popup.repository.PopupSessionQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupSessionView;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupSessionQueryService {

	private final PopupSessionQueryRepository popupSessionQueryRepository;

	@Cacheable(value = "popupSessions", key = "#query.popupId + '_' + #query.from + '_' + #query.to")
	public PopupSessionListResponse getProductSessions(PopupSessionListQuery query) {
		List<PopupSessionView> views = popupSessionQueryRepository.findProductSessions(
				query.getPopupId(), query.getFrom(), query.getTo());

		List<PopupSessionListResponse.ItemDto> items = views.stream()
				.map(view -> PopupSessionListResponse.ItemDto.builder()
						.id(toUuid(view.id()))
						.startAt(view.startAt())
						.endAt(view.endAt())
						.price(view.price())
						.capacity(view.capacity())
						.remainingCapacity(view.remainingCapacity())
						.isActive(view.isActive())
						.build())
				.toList();

		return PopupSessionListResponse.builder()
				.items(items)
				.build();
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}
}
