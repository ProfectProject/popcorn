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

	// Java Bean 규약에 맞는 getter 메서드들 추가
	default String getId() {
		return id();
	}

	default LocalDateTime getStartAt() {
		return startAt();
	}

	default LocalDateTime getEndAt() {
		return endAt();
	}

	default Integer getPrice() {
		return price();
	}

	default Integer getCapacity() {
		return capacity();
	}

	default Integer getRemainingCapacity() {
		return remainingCapacity();
	}

	default Boolean getIsActive() {
		return isActive();
	}
}
