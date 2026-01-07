package com.popcorn.demo.domain.popup.service.owner;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.exception.PopupException;

@Service
public class OwnerPopupValidationService {

	private static final int MIN_TITLE_LENGTH = 1;
	private static final int MAX_TITLE_LENGTH = 200;
	private static final String INVALID_CHARS = "<>\"'&;";

	public String validateAndTrimTitle(String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		String trimmed = title.trim();
		if (trimmed.length() < MIN_TITLE_LENGTH || trimmed.length() > MAX_TITLE_LENGTH) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		if (trimmed.chars().anyMatch(c -> INVALID_CHARS.indexOf(c) >= 0)) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		return trimmed;
	}

	public String validateCreateRequest(CreatePopupRequest request) {
		if (request == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		if (request.getStoreId() == null || request.getCategory() == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		validateCreateSchedules(request.getSchedules());
		return validateAndTrimTitle(request.getTitle());
	}

	public String validateUpdateRequest(UpdatePopupRequest request) {
		if (request == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
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
		if (request.getCreateSchedules() != null || request.getUpdateSchedules() != null
				|| request.getDeleteScheduleIds() != null) {
			hasUpdate = true;
		}
		if (!hasUpdate) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		validateUpdateSchedules(request.getCreateSchedules(), request.getUpdateSchedules(),
				request.getDeleteScheduleIds());
		return trimmedTitle;
	}

	private void validateCreateSchedules(List<CreatePopupScheduleRequest> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		for (CreatePopupScheduleRequest schedule : schedules) {
			if (schedule == null) {
				throw new PopupException(PopupResponseCode.INVALID_REQUEST);
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
					throw new PopupException(PopupResponseCode.INVALID_REQUEST);
				}
				validateScheduleTime(schedule.getStartAt(), schedule.getEndAt());
				validatePrice(schedule.getPrice());
				validateCapacity(schedule.getCapacity());
			}
		}
		if (updateSchedules != null) {
			for (UpdatePopupScheduleRequest schedule : updateSchedules) {
				if (schedule == null || schedule.getScheduleId() == null) {
					throw new PopupException(PopupResponseCode.INVALID_REQUEST);
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
					throw new PopupException(PopupResponseCode.INVALID_REQUEST);
				}
			}
		}
	}

	private void validateScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
		if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}

	private void validatePartialScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
		if ((startAt == null) != (endAt == null)) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		if (startAt != null && !endAt.isAfter(startAt)) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}

	private void validatePrice(Integer price) {
		if (price == null || price < 0) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}

	private void validateCapacity(Integer capacity) {
		if (capacity == null || capacity < 1) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}
}
