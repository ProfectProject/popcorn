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
}
