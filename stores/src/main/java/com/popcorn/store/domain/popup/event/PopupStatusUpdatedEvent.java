package com.popcorn.store.domain.popup.event;

import com.popcorn.store.domain.popup.entity.Popup;
import lombok.Getter;

@Getter
public class PopupStatusUpdatedEvent {

	private final Long ownerId;
	private final Popup popup;

	public PopupStatusUpdatedEvent(Long ownerId, Popup popup) {
		this.ownerId = ownerId;
		this.popup = popup;
	}
}
