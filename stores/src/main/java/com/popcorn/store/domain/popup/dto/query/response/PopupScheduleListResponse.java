package com.popcorn.store.domain.popup.dto.query.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupScheduleListResponse {

	private List<ItemDto> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemDto {
		private UUID id;
		private LocalDateTime startAt;
		private LocalDateTime endAt;
		private Integer price;
		private Integer capacity;
		private Integer remainingCapacity;
		private Boolean isActive;
	}
}
