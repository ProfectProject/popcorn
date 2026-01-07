package com.popcorn.demo.domain.payment.dto.response;

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
public class PaymentDetailResponse {

	private UUID paymentId;
	private UUID orderId;
	private String method;
	private String status;
	private Integer amount;
	private LocalDateTime approvedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String orderStatus;
}
