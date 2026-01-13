package com.popcorn.demo.domain.payment.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentConfirmResponse {

	private UUID paymentId;
	private String status;
	private String orderStatus;
	private UUID orderId;
	private String orderNo;
	private Integer amount;
	private LocalDateTime approvedAt;
}
