package com.popcorn.demo.domain.popup.dto.query.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupListResponse {

	private List<ItemDto> items;
	private int page;
	private int size;
	private long total;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemDto {
		private UUID id;
		private UUID storeId;
		private String title;
		private PopupCategory category;
		private PopupStatus status;
		private LocalDateTime reservationOpenAt;
		private String addressRoad;
		private String addressDetail;
		private LocalDateTime eventStartAt;
		private LocalDateTime eventEndAt;
	}
}
