package com.popcorn.store.domain.popup.event;

import java.util.UUID;

import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleCapacity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopupScheduleReservationEvent {

	public enum Action {
		RESERVE,
		CANCEL,
		FAIL,
		COMPLETE
	}

	private final Action action;
	private final UUID popupId;
	private final UUID scheduleId;
	private final int quantity;
	private PopupScheduleCapacity result;

	public PopupScheduleReservationEvent(Action action, UUID popupId, UUID scheduleId, int quantity) {
		this.action = action;
		this.popupId = popupId;
		this.scheduleId = scheduleId;
		this.quantity = quantity;
	}
}
