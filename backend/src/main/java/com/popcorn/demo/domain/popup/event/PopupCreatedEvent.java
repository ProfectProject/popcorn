package com.popcorn.demo.domain.popup.event;

import com.popcorn.demo.domain.popup.entity.Popup;

public record PopupCreatedEvent(Long ownerId, Popup popup) {

}
