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

	// 🎯 프론트엔드 토스 결제위젯용 추가 필드
	private String clientKey;      // 토스 클라이언트 키
	private String paymentKey;     // 토스 결제키 (결제 식별용)
	private Boolean readyForPayment; // 즉시 결제 가능 여부
}
