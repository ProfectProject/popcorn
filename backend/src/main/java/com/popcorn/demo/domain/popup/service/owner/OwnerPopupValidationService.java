package com.popcorn.demo.domain.popup.service.owner;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.popup.dto.owner.OwnerPopupResponseCode;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.exception.owner.OwnerPopupException;
@Service
public class OwnerPopupValidationService {

	private static final int MIN_TITLE_LENGTH = 1;
	private static final int MAX_TITLE_LENGTH = 200;
	private static final String INVALID_CHARS = "<>\"'&;";

	public String validateAndTrimTitle(String title) {
		if (title == null || title.trim().isEmpty()) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.TITLE_REQUIRED);
		}

		String trimmed = title.trim();
		if (trimmed.length() < MIN_TITLE_LENGTH || trimmed.length() > MAX_TITLE_LENGTH) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.TITLE_LENGTH_INVALID);
		}

		if (trimmed.chars().anyMatch(c -> INVALID_CHARS.indexOf(c) >= 0)) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.TITLE_INVALID_CHARS);
		}

		return trimmed;
	}

	public String validateCreateRequest(CreatePopupRequest request) {
		if (request == null) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.REQUEST_BODY_REQUIRED);
		}
		if (request.getStoreId() == null || request.getCategory() == null) {
			if (request.getStoreId() == null) {
				throw OwnerPopupException.of(OwnerPopupResponseCode.STORE_ID_REQUIRED);
			}
			throw OwnerPopupException.of(OwnerPopupResponseCode.CATEGORY_REQUIRED);
		}
		validateCreateSchedules(request.getSchedules());
		return validateAndTrimTitle(request.getTitle());
	}

	public String validateUpdateRequest(UpdatePopupRequest request) {
		if (request == null) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.REQUEST_BODY_REQUIRED);
		}
		boolean hasUpdate = false;
		String trimmedTitle = null;
		if (request.getTitle() != null) {
			trimmedTitle = validateAndTrimTitle(request.getTitle());
			hasUpdate = true;
		}
		if (request.getDescription() != null || request.getPopupCategory() != null) {
			hasUpdate = true;
		}
		if (request.getReservationOpenAt() != null || request.getAddressRoad() != null
				|| request.getAddressDetail() != null) {
			hasUpdate = true;
		}
		if (request.getCreateSchedules() != null || request.getUpdateSchedules() != null
				|| request.getDeleteScheduleIds() != null) {
			hasUpdate = true;
		}
		if (!hasUpdate) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.UPDATE_NO_CHANGES);
		}
		validateUpdateSchedules(request.getCreateSchedules(), request.getUpdateSchedules(),
				request.getDeleteScheduleIds());
		return trimmedTitle;
	}

	private void validateCreateSchedules(List<CreatePopupScheduleRequest> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULES_REQUIRED);
		}
		for (CreatePopupScheduleRequest schedule : schedules) {
			if (schedule == null) {
				throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULE_REQUIRED);
			}
			validateScheduleTime(schedule.getStartAt(), schedule.getEndAt());
			validatePrice(schedule.getPrice());
			validateCapacity(schedule.getCapacity());
		}
	}

	private void validateUpdateSchedules(List<CreatePopupScheduleRequest> createSchedules,
			List<UpdatePopupScheduleRequest> updateSchedules,
			List<UUID> deleteScheduleIds) {
		if (createSchedules != null) {
			for (CreatePopupScheduleRequest schedule : createSchedules) {
				if (schedule == null) {
					throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULE_REQUIRED);
				}
				validateScheduleTime(schedule.getStartAt(), schedule.getEndAt());
				validatePrice(schedule.getPrice());
				validateCapacity(schedule.getCapacity());
			}
		}
		if (updateSchedules != null) {
			for (UpdatePopupScheduleRequest schedule : updateSchedules) {
				if (schedule == null || schedule.getScheduleId() == null) {
					if (schedule == null) {
						throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULE_REQUIRED);
					}
					throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULE_ID_REQUIRED);
				}
				validatePartialScheduleTime(schedule.getStartAt(), schedule.getEndAt());
				if (schedule.getPrice() != null) {
					validatePrice(schedule.getPrice());
				}
				if (schedule.getCapacity() != null) {
					validateCapacity(schedule.getCapacity());
				}
			}
		}
		if (deleteScheduleIds != null) {
			for (UUID scheduleId : deleteScheduleIds) {
				if (scheduleId == null) {
					throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULE_DELETE_ID_REQUIRED);
				}
			}
		}
	}

	private void validateScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
		if (startAt == null) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.START_AT_REQUIRED);
		}
		if (endAt == null) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.END_AT_REQUIRED);
		}
		if (!endAt.isAfter(startAt)) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.END_BEFORE_START);
		}
	}

	private void validatePartialScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
		if ((startAt == null) != (endAt == null)) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.SCHEDULE_TIME_PAIR_REQUIRED);
		}
		if (startAt != null && !endAt.isAfter(startAt)) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.END_BEFORE_START);
		}
	}

	private void validatePrice(Integer price) {
		if (price == null) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.PRICE_REQUIRED);
		}
		if (price < 0) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.PRICE_MIN_INVALID);
		}
	}

	private void validateCapacity(Integer capacity) {
		if (capacity == null) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.CAPACITY_REQUIRED);
		}
		if (capacity < 1) {
			throw OwnerPopupException.of(OwnerPopupResponseCode.CAPACITY_MIN_INVALID);
		}
	}
}
