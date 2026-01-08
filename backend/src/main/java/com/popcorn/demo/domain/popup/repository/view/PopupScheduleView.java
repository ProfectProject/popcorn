package com.popcorn.demo.domain.popup.repository.view;

import java.time.LocalDateTime;

public interface PopupScheduleView {

	String id();

	LocalDateTime startAt();

	LocalDateTime endAt();

	Integer price();

	Integer capacity();

	Integer remainingCapacity();

	Boolean isActive();
}
