package com.popcorn.demo.domain.order.dto.response;

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
public class OrderStatusDto {

	private UUID orderId;
	private String orderNo;
	private String status;
	private String paymentStatus;
	private LocalDateTime cancelableUntil;
	private LocalDateTime updatedAt;
}
