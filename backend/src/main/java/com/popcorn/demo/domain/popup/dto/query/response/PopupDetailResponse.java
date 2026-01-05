package com.popcorn.demo.domain.popup.dto.query.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupDetailResponse {

	private UUID id;
	private UUID storeId;
	private String title;
	private String productType;
	private String category;
	private Long regionId;
	private boolean isHidden;
	private LocalDateTime eventStartAt;
	private LocalDateTime eventEndAt;
	private LocationDto location;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LocationDto {
		private UUID id;
		private String name;
		private String address1;
		private String address2;
		private Double latitude;
		private Double longitude;
		private String placeNote;
	}
}
