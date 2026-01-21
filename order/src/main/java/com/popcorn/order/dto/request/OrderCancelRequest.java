package com.popcorn.order.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

/**
 * 주문 취소 요청 DTO
 *
 * 팝업 스토어 또는 고객이 주문을 취소할 때 사용됩니다.
 */
@Getter
@Builder
@Schema(description = "주문 취소 요청 - 고객, 스토어, 관리자가 주문을 취소할 때 사용")
public class OrderCancelRequest {

    /** 취소할 주문 ID */
    @Schema(description = "취소할 주문 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "주문 ID는 필수입니다.")
    private final UUID orderId;

    /** 팝업 ID (백엔드 호환성을 위해) */
    @Schema(description = "팝업 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private final UUID popupId;

    /** 주문 번호 (선택적, 검증용) */
    @Schema(description = "주문 번호", example = "O-20241201-001")
    private final String orderNo;

    /** 취소 사유 */
    @Schema(description = "취소 사유", example = "고객 변심으로 인한 취소")
    @NotNull(message = "취소 사유는 필수입니다.")
    @Size(min = 1, max = 500, message = "취소 사유는 1자 이상 500자 이하여야 합니다.")
    private final String cancelReason;

    /** 취소 주체 (CUSTOMER, STORE, MANAGER, SYSTEM) */
    @Schema(description = "취소 주체", example = "CUSTOMER", allowableValues = {"CUSTOMER", "STORE", "MANAGER", "SYSTEM"})
    @NotNull(message = "취소 주체는 필수입니다.")
    private final String cancelledBy;

    /** 취소 요청자 ID (고객 ID 또는 관리자 ID) */
    @Schema(description = "취소 요청자 ID", example = "12345")
    private final Long requesterId;

    /** 환불 처리 여부 */
    @Schema(description = "환불 처리 여부", example = "true")
    @Builder.Default
    private final Boolean processRefund = true;

    /** 관리자 강제 취소 여부 */
    @Schema(description = "관리자 강제 취소 여부", example = "false")
    @Builder.Default
    private final Boolean forceCancel = false;
}