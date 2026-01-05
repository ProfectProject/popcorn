package com.popcorn.demo.domain.popup.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopupViewedEvent {

	private final UUID productId;
	private final UUID storeId;
	private final String category;
	private final Long regionId;
	private final LocalDateTime occurredAt;
}
