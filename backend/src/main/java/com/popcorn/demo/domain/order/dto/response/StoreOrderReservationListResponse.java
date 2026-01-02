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
public class StoreOrderReservationListResponse {

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
		private String reservationNo;
		private String status;
		private Integer totalAmount;
		private LocalDateTime cancelableUntil;
		private LocalDateTime createdAt;
	}
}
