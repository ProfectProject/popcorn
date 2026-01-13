package com.popcorn.demo.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentConfirmRequest {

	@NotBlank(message = "paymentKey는 필수입니다.")
	private String paymentKey;

	@NotBlank(message = "orderId는 필수입니다.")
	private String orderId;

	@NotNull(message = "amount는 필수입니다.")
	private Integer amount;
}
