package com.popcorn.demo.domain.popup.event;

import com.popcorn.demo.domain.popup.entity.Popup;
import lombok.Getter;

@Getter
public class PopupCreatedEvent {

	private final Long ownerId;
	private final Popup popup;

	public PopupCreatedEvent(Long ownerId, Popup popup) {
		this.ownerId = ownerId;
		this.popup = popup;
	}
}
