package com.popcorn.demo.domain.popup.repository.owner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.popup.repository.owner.view.OwnerPopupScheduleView;

public interface OwnerPopupScheduleRepository {

	void insertSchedule(UUID scheduleId, UUID popupId, LocalDateTime startAt, LocalDateTime endAt,
			Integer price, Integer capacity, Integer remainingCapacity, boolean active,
			LocalDateTime now, Long createdBy, Long updatedBy);

	int updateSchedule(UUID scheduleId, UUID popupId, LocalDateTime startAt, LocalDateTime endAt,
			Integer price, Integer capacity, Boolean active, LocalDateTime now, Long updatedBy);

	int softDeleteSchedule(UUID scheduleId, UUID popupId, LocalDateTime now, Long deletedBy);

	int softDeleteSchedulesByPopup(UUID popupId, LocalDateTime now, Long deletedBy);

	int deactivateActiveSchedulesByPopup(UUID popupId, LocalDateTime now, Long updatedBy);

	List<OwnerPopupScheduleView> findSchedulesByPopup(UUID popupId);

	boolean existsSchedule(UUID scheduleId, UUID popupId);
}
