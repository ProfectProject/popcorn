package com.popcorn.demo.domain.checkin.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public record CheckinRow(
		UUID checkinId,
		UUID orderId,
		UUID orderQrCodeId,
		String qrCode,
		LocalDateTime createdAt,
		Long createdBy) {
}
