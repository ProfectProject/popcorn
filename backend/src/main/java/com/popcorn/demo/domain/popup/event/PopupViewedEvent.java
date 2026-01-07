package com.popcorn.demo.domain.popup.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopupViewedEvent {

	private final UUID popupId;
	private final UUID storeId;
	private final PopupCategory category;
	private final Long regionId;
	private final LocalDateTime occurredAt;
}
