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
public class ReservationPaymentCreateResponse {

	private UUID paymentId;
	private String paymentStatus;
	private String orderStatus;
	private LocalDateTime approvedAt;
}
