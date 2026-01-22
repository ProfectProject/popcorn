package com.popcorn.demo.domain.popup.repository.owner.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OwnerPopupScheduleView {

	UUID getScheduleId();
	LocalDateTime getStartAt();
	LocalDateTime getEndAt();
	Integer getPrice();
	Integer getCapacity();
	Integer getRemainingCapacity();
	boolean isActive();
}
