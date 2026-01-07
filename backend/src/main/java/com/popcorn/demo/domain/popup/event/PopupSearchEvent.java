package com.popcorn.demo.domain.popup.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopupSearchEvent {

	private final Long regionId;
	private final PopupCategory category;
	private final String keyword;
	private final UUID storeId;
	private final Integer page;
	private final Integer size;
	private final long total;
	private final LocalDateTime occurredAt;
}
