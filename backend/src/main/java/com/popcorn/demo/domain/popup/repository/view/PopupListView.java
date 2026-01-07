package com.popcorn.demo.domain.popup.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PopupListView {

	String id();

	String storeId();

	String title();

	String description();

	String category();

	String status();

	LocalDateTime eventStartAt();

	LocalDateTime eventEndAt();

	// Java Bean 규약에 맞는 getter 메서드들 추가
	default String getId() {
		return id();
	}

	default String getStoreId() {
		return storeId();
	}

	default String getTitle() {
		return title();
	}

	default String getDescription() {
		return description();
	}

	default String getCategory() {
		return category();
	}

	default String getStatus() {
		return status();
	}

	default LocalDateTime getEventStartAt() {
		return eventStartAt();
	}

	default LocalDateTime getEventEndAt() {
		return eventEndAt();
	}
}
