package com.popcorn.demo.domain.order.dto.response;

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
public class MyOrderTimelineResponse {

	private List<ItemDto> items;
	private int page;
	private int size;
	private long total;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemDto {
		private String type;
		private UUID id;
		private String orderNo;
		private String status;
		private Integer totalAmount;
		private LocalDateTime cancelableUntil;
		private LocalDateTime createdAt;
		private UUID popupId;
		private UUID storeId;
		private String title;
		private LocalDateTime sessionStartAt;
		private LocationDto location;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LocationDto {
		private String name;
		private String address1;
		private String address2;
	}
}
