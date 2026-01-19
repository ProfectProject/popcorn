package com.popcorn.demo.domain.payment.toss;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TossPaymentsCancelRequest {
	private String cancelReason;
}