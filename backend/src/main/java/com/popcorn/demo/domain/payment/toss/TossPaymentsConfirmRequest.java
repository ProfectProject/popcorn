package com.popcorn.demo.domain.payment.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentsConfirmRequest {
	private String paymentKey;
	private String orderId;
	private Integer amount;
}
