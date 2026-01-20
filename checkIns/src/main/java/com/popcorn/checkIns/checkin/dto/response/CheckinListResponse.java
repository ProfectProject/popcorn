package com.popcorn.checkIns.checkin.dto.response;

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
public class CheckinListResponse {

	private int count;
	private List<Item> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Item {
		private UUID checkinId;
		private UUID orderId;
		private String qrCode;
		private LocalDateTime createdAt;
	}
}
