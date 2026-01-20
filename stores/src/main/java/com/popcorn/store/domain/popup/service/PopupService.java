package com.popcorn.store.domain.popup.service;

import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleCapacity;
import com.popcorn.store.domain.popup.exception.PopupException;
import com.popcorn.store.domain.popup.repository.PopupScheduleReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.store.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.store.domain.popup.dto.query.PopupListQuery;
import com.popcorn.store.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.store.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.store.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopupService {

	private final PopupQueryService popupQueryService;
	private final PopupScheduleQueryService popupScheduleQueryService;
	private final PopupValidationService popupValidationService;
	private final PopupScheduleReservationRepository popupScheduleReservationRepository;

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
	public PopupScheduleCapacity reservationPopupSchedule(UUID popupId,UUID scheduleId, Integer quantity) {
		log.info("[POPUP_SCHEDULE_RESERVE] popupId={}, scheduleId={}, quantity={}", popupId, scheduleId, quantity);
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.reserveCapacity(popupId, scheduleId, quantity);
		if (capacity == null) {
			log.warn("[POPUP_SCHEDULE_RESERVE_FAILED] popupId={}, scheduleId={}, quantity={}", popupId, scheduleId, quantity);
			throw PopupException.insufficientReservationCapacity();
		}
		return capacity;
	}

	@Transactional
	public PopupScheduleCapacity cancelPopupScheduleReservation(UUID scheduleId, Integer quantity) {
		log.info("[POPUP_SCHEDULE_CANCEL] scheduleId={}, quantity={}", scheduleId, quantity);
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.cancelCapacity(scheduleId, quantity);
		if (capacity == null) {
			log.warn("[POPUP_SCHEDULE_CANCEL_FAILED] scheduleId={}, quantity={}", scheduleId, quantity);
			throw PopupException.insufficientReservationCapacity();
		}
		return capacity;
	}

	@Transactional
	public PopupScheduleCapacity failPopupScheduleReservation(UUID scheduleId, Integer quantity) {
		log.info("[POPUP_SCHEDULE_FAIL] scheduleId={}, quantity={}", scheduleId, quantity);
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.failCapacity(scheduleId, quantity);
		if (capacity == null) {
			log.warn("[POPUP_SCHEDULE_FAIL_FAILED] scheduleId={}, quantity={}", scheduleId, quantity);
			throw PopupException.insufficientReservationCapacity();
		}
		return capacity;
	}

	@Transactional
	public PopupScheduleCapacity completePopupScheduleReservation(UUID scheduleId, Integer quantity) {
		log.info("[POPUP_SCHEDULE_COMPLETE] scheduleId={}, quantity={}", scheduleId, quantity);
		PopupScheduleCapacity capacity = popupScheduleReservationRepository.completeCapacity(scheduleId, quantity);
		if (capacity == null) {
			log.warn("[POPUP_SCHEDULE_COMPLETE_FAILED] scheduleId={}, quantity={}", scheduleId, quantity);
			throw PopupException.insufficientReservationCapacity();
		}
		return capacity;
	}

}
