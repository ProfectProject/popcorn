package com.popcorn.demo.domain.payment.dto.response;

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
public class PaymentListResponse {

	private int count;
	private List<Item> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Item {
		private UUID paymentId;
		private UUID orderId;
		private String method;
		private String status;
		private Integer amount;
		private LocalDateTime approvedAt;
		private LocalDateTime createdAt;
	}
}
