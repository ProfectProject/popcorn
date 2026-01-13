package com.popcorn.demo.domain.payment.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class TossPaymentsConfirmResponse {
	private String paymentKey;
	private String orderId;
	private Integer totalAmount;
	private String status;
	private String method;
	private String requestedAt;
	private String approvedAt;
}
