package com.popcorn.demo.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

	@NotBlank(message = "상태는 필수입니다.")
	private String status;

	@NotBlank(message = "사유는 필수입니다.")
	private String reason;
}
