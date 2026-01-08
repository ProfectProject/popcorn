package com.popcorn.demo.domain.popup.repository.view;

import java.time.LocalDateTime;

public interface PopupScheduleView {

	String getScheduleId();

	LocalDateTime getStartAt();

	LocalDateTime getEndAt();

	Integer getPrice();

	Integer getCapacity();

	Integer getRemainingCapacity();

	Boolean getIsActive();
}
