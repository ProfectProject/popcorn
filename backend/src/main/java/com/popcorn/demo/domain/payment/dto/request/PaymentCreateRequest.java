package com.popcorn.demo.domain.payment.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {

	@NotBlank(message = "결제 수단은 필수입니다.")
	@Schema(example = "CARD", defaultValue = "CARD")
	private String method;

	@NotNull(message = "결제 금액은 필수입니다.")
	@Schema(example = "4000", defaultValue = "4000")
	private Integer amount;

	@Schema(example = "{\"pg\":\"example\",\"transactionId\":\"T-20250101\"}", defaultValue = "{\"pg\":\"example\",\"transactionId\":\"T-20250101\"}")
	private Map<String, Object> rawPayload;
}
