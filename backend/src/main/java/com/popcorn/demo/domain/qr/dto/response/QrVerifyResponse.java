package com.popcorn.demo.domain.qr.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrVerifyResponse {

	private boolean valid;
	private java.util.UUID checkinId;
	private UUID orderId;
	private String qrCode;
	private LocalDateTime expiresAt;
}
