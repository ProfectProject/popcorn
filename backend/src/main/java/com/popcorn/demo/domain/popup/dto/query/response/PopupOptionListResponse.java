package com.popcorn.demo.domain.popup.dto.query.response;

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
public class PopupOptionListResponse {

	private List<ItemDto> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemDto {
		private UUID id;
		private String name;
		private Integer price;
		private Integer capacity;
		private boolean isHidden;
	}
}
