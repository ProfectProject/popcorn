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
public class PaymentCreateResponse {

	private UUID paymentId;
	private String status;
	private String orderStatus;
	private UUID orderId;
	private String orderNo;
	private Integer amount;
	private String customerKey;
	private String successUrl;
	private String failUrl;
	private String paymentToken; // JWT 암호화된 결제 토큰
	private LocalDateTime approvedAt;
}
