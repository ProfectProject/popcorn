package com.popcorn.demo.domain.qr.checkin.dto.response;

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
public class CheckinDetailResponse {

	private UUID checkinId;
	private UUID orderId;
	private UUID orderQrCodeId;
	private String qrCode;
	private LocalDateTime createdAt;
	private Long createdBy;
}
