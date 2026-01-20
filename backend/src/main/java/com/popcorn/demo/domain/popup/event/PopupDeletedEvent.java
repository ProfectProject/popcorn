package com.popcorn.demo.domain.popup.event;

import com.popcorn.demo.domain.popup.entity.Popup;

public record PopupDeletedEvent(Long ownerId, Popup popup) {

}
