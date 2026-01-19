package com.popcorn.demo.domain.popup.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

@Getter
public class PopupScheduleCreatedEvent extends BasePopupScheduleEvent {

	private final LocalDateTime startAt;
	private final LocalDateTime endAt;
	private final Integer price;
	private final Integer capacity;

	public PopupScheduleCreatedEvent(
			Long ownerId,
			UUID popupId,
			UUID scheduleId,
			LocalDateTime startAt,
			LocalDateTime endAt,
			Integer price,
			Integer capacity) {
		super(
				popupId,
				scheduleId,
				"popup_schedule_created",
				ownerId,
				Map.of(
						"startAt", startAt,
						"endAt", endAt,
						"price", price,
						"capacity", capacity
				));
		this.startAt = startAt;
		this.endAt = endAt;
		this.price = price;
		this.capacity = capacity;
	}

	@Override
	protected Map<String, Object> getEventPayload() {
		return Map.of(
				"popupId", getPopupId(),
				"scheduleId", getScheduleId(),
				"ownerId", getOwnerId(),
				"startAt", startAt,
				"endAt", endAt,
				"price", price,
				"capacity", capacity,
				"createdAt", getTimestamp()
		);
	}
}
