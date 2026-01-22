package com.popcorn.store.domain.popup.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.store.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleListResponse;
import com.popcorn.store.domain.popup.exception.PopupException;
import com.popcorn.store.domain.popup.repository.PopupQueryRepository;
import com.popcorn.store.domain.popup.repository.PopupScheduleQueryRepository;
import com.popcorn.store.domain.popup.repository.view.PopupScheduleView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PopupScheduleQueryService {
	private final PopupScheduleQueryRepository popupScheduleQueryRepository;
	private final PopupQueryRepository popupQueryRepository;

	@Cacheable(value = "popupSessions", key = "#query.popupId + '_' + #query.from + '_' + #query.to")
	public PopupScheduleListResponse getProductSessions(PopupScheduleListQuery query) {
		// 먼저 popup이 존재하는지 확인
		if (popupQueryRepository.findPopupDetail(query.getPopupId()).isEmpty()) {
			throw PopupException.popupNotFound();
		}

		List<PopupScheduleView> views = popupScheduleQueryRepository.findProductSessions(
				query.getPopupId(), query.getFrom(), query.getTo());

		List<PopupScheduleListResponse.ItemDto> items = views.stream()
				.map(view -> {
					UUID scheduleId = toUuid(view.getScheduleId());
					if (scheduleId == null) {
						return null; // 유효하지 않은 UUID는 null로 처리
					}
					return PopupScheduleListResponse.ItemDto.builder()
							.id(scheduleId)
							.startAt(view.getStartAt())
							.endAt(view.getEndAt())
							.price(view.getPrice())
							.capacity(view.getCapacity())
							.remainingCapacity(view.getRemainingCapacity())
							.isActive(view.getIsActive())
							.build();
				})
				.filter(Objects::nonNull) // null 아이템들을 필터링
				.toList();

		return PopupScheduleListResponse.builder()
				.items(items)
				.build();
	}

	private UUID toUuid(String value) {
		if (value == null) {
			return null;
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ex) {
			// UUID 형식이 잘못된 경우 로그를 남기고 null을 반환
			log.warn("잘못된 UUID 형식의 스케줄 ID가 발견되어 제외됩니다: {}", value, ex);
			return null;
		}
	}
}
