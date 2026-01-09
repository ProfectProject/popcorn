package com.popcorn.demo.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusUpdateRequest {

	@NotBlank(message = "결제 상태는 필수입니다.")
	@Schema(example = "PAID", defaultValue = "PAID")
	private String status;
}
