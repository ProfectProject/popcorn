package com.popcorn.demo.domain.popup.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PopupListView {

	String getId();

	String getStoreId();

	String getTitle();

	String getDescription();

	String getCategory();

	String getStatus();

	LocalDateTime getEventStartAt();

	LocalDateTime getEventEndAt();
}
