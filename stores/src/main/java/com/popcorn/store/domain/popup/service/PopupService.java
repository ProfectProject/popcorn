package com.popcorn.store.domain.popup.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.store.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.store.domain.popup.dto.query.PopupListQuery;
import com.popcorn.store.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.store.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.store.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleCapacity;
import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleListResponse;
import com.popcorn.store.domain.popup.event.PopupScheduleReservationEvent;
import com.popcorn.store.domain.popup.exception.PopupException;
import com.popcorn.store.domain.popup.repository.PopupScheduleReservationRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PopupService {

	private final PopupQueryService popupQueryService;
	private final PopupScheduleQueryService popupScheduleQueryService;
	private final PopupValidationService popupValidationService;
	private final PopupScheduleReservationRepository popupScheduleReservationRepository;
	private final ApplicationEventPublisher eventPublisher;
	public PopupListResponse getPopups(PopupListQuery query) {
		PopupListQuery normalizedQuery = popupValidationService.normalizeListQuery(query);
		PopupListResponse response = popupQueryService.getPopups(normalizedQuery);

		return response;
	}

	public PopupDetailResponse getPopupDetail(PopupDetailQuery query) {
		popupValidationService.validateDetailQuery(query);
		PopupDetailResponse response = popupQueryService.getPopupDetail(query);

		return response;
	}

	public PopupScheduleListResponse getProductSessions(PopupScheduleListQuery query) {
		PopupScheduleListQuery normalizedQuery = popupValidationService.normalizeSessionQuery(query);
		return popupScheduleQueryService.getProductSessions(normalizedQuery);
	}

	@Transactional
	public PopupScheduleCapacity reservationPopupSchedule(UUID popupId, UUID scheduleId, Integer quantity) {
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.reserveCapacity(popupId, scheduleId, quantity);
		if (capacity == null) {
			throw PopupException.insufficientReservationCapacity();
		}
		eventPublisher.publishEvent(new PopupScheduleReservationEvent(
				PopupScheduleReservationEvent.Action.RESERVE, popupId, scheduleId, quantity));
		return capacity;
	}

	@Transactional
	public PopupScheduleCapacity cancelPopupScheduleReservation(UUID scheduleId, Integer quantity) {
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.cancelCapacity(scheduleId, quantity);
		if (capacity == null) {
			throw PopupException.insufficientReservationCapacity();
		}
		eventPublisher.publishEvent(new PopupScheduleReservationEvent(
				PopupScheduleReservationEvent.Action.CANCEL, null, scheduleId, quantity));
		return capacity;
	}

	@Transactional
	public PopupScheduleCapacity failPopupScheduleReservation(UUID scheduleId, Integer quantity) {
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.failCapacity(scheduleId, quantity);
		if (capacity == null) {
			throw PopupException.insufficientReservationCapacity();
		}
		eventPublisher.publishEvent(new PopupScheduleReservationEvent(
				PopupScheduleReservationEvent.Action.FAIL, null, scheduleId, quantity));
		return capacity;
	}

	@Transactional
	public PopupScheduleCapacity completePopupScheduleReservation(UUID scheduleId, Integer quantity) {
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.completeCapacity(scheduleId, quantity);
		if (capacity == null) {
			throw PopupException.insufficientReservationCapacity();
		}
		eventPublisher.publishEvent(new PopupScheduleReservationEvent(
				PopupScheduleReservationEvent.Action.COMPLETE, null, scheduleId, quantity));
		return capacity;
	}

}
