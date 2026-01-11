package com.popcorn.demo.domain.payment.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListResponse {

	@Schema(example = "2")
	private int count;
	private List<Item> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Item {
		@Schema(example = "70000000-0000-0000-0000-000000000003")
		private UUID paymentId;
		@Schema(example = "40000000-0000-0000-0000-000000000003")
		private UUID orderId;
		@Schema(example = "CARD")
		private String method;
		@Schema(example = "READY")
		private String status;
		@Schema(example = "35000")
		private Integer amount;
		@Schema(example = "2026-01-07T15:25:00")
		private LocalDateTime approvedAt;
		@Schema(example = "2026-01-07T15:20:00")
		private LocalDateTime createdAt;
	}
}
