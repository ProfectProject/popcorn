package com.popcorn.demo.domain.popup.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

@Getter
public class PopupScheduleUpdatedEvent extends BasePopupScheduleEvent {

	private final LocalDateTime startAt;
	private final LocalDateTime endAt;
	private final Integer price;
	private final Integer capacity;
	private final Boolean active;

	public PopupScheduleUpdatedEvent(
			Long ownerId,
			UUID popupId,
			UUID scheduleId,
			LocalDateTime startAt,
			LocalDateTime endAt,
			Integer price,
			Integer capacity,
			Boolean active) {
		super(
				popupId,
				scheduleId,
				"popup_schedule_updated",
				ownerId,
				buildMetadata(startAt, endAt, price, capacity, active));
		this.startAt = startAt;
		this.endAt = endAt;
		this.price = price;
		this.capacity = capacity;
		this.active = active;
	}

	@Override
	protected Map<String, Object> getEventPayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("popupId", getPopupId());
		payload.put("scheduleId", getScheduleId());
		payload.put("ownerId", getOwnerId());
		putIfNotNull(payload, "startAt", startAt);
		putIfNotNull(payload, "endAt", endAt);
		putIfNotNull(payload, "price", price);
		putIfNotNull(payload, "capacity", capacity);
		putIfNotNull(payload, "active", active);
		payload.put("updatedAt", getTimestamp());
		return payload;
	}

	private static Map<String, Object> buildMetadata(LocalDateTime startAt,
			LocalDateTime endAt,
			Integer price,
			Integer capacity,
			Boolean active) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		putIfNotNull(metadata, "startAt", startAt);
		putIfNotNull(metadata, "endAt", endAt);
		putIfNotNull(metadata, "price", price);
		putIfNotNull(metadata, "capacity", capacity);
		putIfNotNull(metadata, "active", active);
		return metadata;
	}

	private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
		if (value != null) {
			target.put(key, value);
		}
	}
}
