package com.popcorn.demo.domain.popup.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupScheduleListResponse;
import com.popcorn.demo.domain.popup.repository.PopupScheduleQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupScheduleView;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupScheduleQueryService {

	private final PopupScheduleQueryRepository popupScheduleQueryRepository;

	@Cacheable(value = "popupSessions", key = "#query.popupId + '_' + #query.from + '_' + #query.to")
	public PopupScheduleListResponse getProductSessions(PopupScheduleListQuery query) {
		List<PopupScheduleView> views = popupScheduleQueryRepository.findProductSessions(
				query.getPopupId(), query.getFrom(), query.getTo());

		List<PopupScheduleListResponse.ItemDto> items = views.stream()
				.map(view -> PopupScheduleListResponse.ItemDto.builder()
						.id(toUuid(view.id()))
						.startAt(view.startAt())
						.endAt(view.endAt())
						.price(view.price())
						.capacity(view.capacity())
						.remainingCapacity(view.remainingCapacity())
						.isActive(view.isActive())
						.build())
				.toList();

		return PopupScheduleListResponse.builder()
				.items(items)
				.build();
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}
}
