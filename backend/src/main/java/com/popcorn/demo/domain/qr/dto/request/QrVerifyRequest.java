package com.popcorn.demo.domain.qr.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrVerifyRequest {

	@NotBlank(message = "qrCode는 필수입니다.")
	private String qrCode;
}
