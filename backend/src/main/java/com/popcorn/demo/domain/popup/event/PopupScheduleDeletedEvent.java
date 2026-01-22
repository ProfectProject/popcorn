package com.popcorn.demo.domain.popup.event;

import java.util.Map;
import java.util.UUID;

import lombok.Getter;

@Getter
public class PopupScheduleDeletedEvent extends BasePopupScheduleEvent {

	public PopupScheduleDeletedEvent(Long ownerId, UUID popupId, UUID scheduleId) {
		super(
				popupId,
				scheduleId,
				"popup_schedule_deleted",
				ownerId
		);
	}

	@Override
	protected Map<String, Object> getEventPayload() {
		return Map.of(
				"popupId", getPopupId(),
				"scheduleId", getScheduleId(),
				"ownerId", getOwnerId(),
				"deletedAt", getTimestamp()
		);
	}
}
