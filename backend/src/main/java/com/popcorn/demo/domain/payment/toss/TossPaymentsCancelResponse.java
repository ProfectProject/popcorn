package com.popcorn.demo.domain.payment.toss;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class TossPaymentsCancelResponse {
	private String paymentKey;
	private String orderId;
	private String status;
	private Integer totalAmount;
	private Integer balanceAmount;
	private String method;
	private String requestedAt;
	private String approvedAt;
	private List<CancelDetail> cancels;

	@Getter
	@Setter
	public static class CancelDetail {
		private Integer cancelAmount;
		private String cancelReason;
		private String canceledAt;
	}
}