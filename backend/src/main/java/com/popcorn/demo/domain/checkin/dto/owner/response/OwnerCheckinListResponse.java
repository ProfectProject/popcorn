package com.popcorn.demo.domain.checkin.dto.owner.response;

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
public class OwnerCheckinListResponse {

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
