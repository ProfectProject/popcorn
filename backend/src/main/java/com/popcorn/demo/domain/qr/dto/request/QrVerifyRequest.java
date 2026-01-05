package com.popcorn.demo.domain.qr.dto.request;

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
public class QrVerifyRequest {

	@NotBlank(message = "qrCode는 필수입니다.")
	@Schema(description = "QR 코드 문자열", example = "qr-test-003")
	private String qrCode;
}
