package com.popcorn.demo.domain.payment.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponse {

	@Schema(example = "70000000-0000-0000-0000-000000000001")
	private UUID paymentId;
	@Schema(example = "40000000-0000-0000-0000-000000000004")
	private UUID orderId;
	@Schema(example = "CARD")
	private String method;
	@Schema(example = "PAID")
	private String status;
	@Schema(example = "18000")
	private Integer amount;
	@Schema(example = "2026-01-07T15:30:00")
	private LocalDateTime approvedAt;
	@Schema(example = "2026-01-07T15:25:00")
	private LocalDateTime createdAt;
	@Schema(example = "2026-01-07T15:30:00")
	private LocalDateTime updatedAt;
	@Schema(example = "PAID")
	private String orderStatus;

	// QR 코드 관련 정보 (예약 주문이고 결제 완료된 경우에만 제공)
	@Schema(example = "true", description = "QR 코드 사용 가능 여부")
	private Boolean qrAvailable;
	@Schema(example = "abc123-def456-ghi789", description = "QR 코드 (qrAvailable이 true인 경우에만 제공)")
	private String qrCode;
	@Schema(example = "2026-01-13T14:00:00", description = "QR 코드 만료 시각")
	private LocalDateTime qrExpiresAt;
}
