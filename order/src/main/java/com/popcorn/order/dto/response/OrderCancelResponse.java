package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 취소 응답 DTO
 *
 * 주문 취소 처리 결과를 반환합니다.
 */
@Getter
@Builder
public class OrderCancelResponse {

    /** 취소된 주문 ID */
    private final UUID orderId;

    /** 팝업 ID (백엔드 호환성을 위해) */
    private final UUID popupId;

    /** 주문 번호 */
    private final String orderNo;

    /** 취소 후 주문 상태 */
    private final String status;

    /** 취소 처리 시간 */
    private final LocalDateTime cancelledAt;

    /** 취소 사유 */
    private final String cancelReason;

    /** 백엔드 호환성을 위한 사유 */
    private final String reason;

    /** 취소 주체 */
    private final String cancelledBy;

    /** 환불 처리 상태 */
    private final String refundStatus;

    /** 환불 예상 금액 */
    private final Integer refundAmount;

    /** 환불 처리 예상 시간 */
    private final LocalDateTime estimatedRefundTime;

    /** 취소 처리 성공 여부 */
    private final Boolean success;

    /** 처리 메시지 */
    private final String message;

    /** 고객에게 보여질 안내 메시지 */
    private final String customerMessage;

    /** 백엔드 호환성을 위한 reason getter */
    public String getReason() {
        return cancelReason;
    }

    /**
     * 성공적인 취소 응답 생성
     */
    public static OrderCancelResponse success(
            UUID orderId,
            String orderNo,
            String cancelReason,
            String cancelledBy,
            Integer refundAmount) {
        return OrderCancelResponse.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .status("CANCELLED")
                .cancelledAt(LocalDateTime.now())
                .cancelReason(cancelReason)
                .cancelledBy(cancelledBy)
                .refundStatus("PROCESSING")
                .refundAmount(refundAmount)
                .estimatedRefundTime(LocalDateTime.now().plusHours(2))
                .success(true)
                .message("주문이 성공적으로 취소되었습니다.")
                .customerMessage("주문이 취소되었습니다. 결제 수단으로 2시간 내에 환불됩니다.")
                .build();
    }

    /**
     * 실패한 취소 응답 생성
     */
    public static OrderCancelResponse failure(UUID orderId, String orderNo, String errorMessage) {
        return OrderCancelResponse.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .success(false)
                .message(errorMessage)
                .customerMessage("주문 취소 처리 중 문제가 발생했습니다. 고객센터로 문의해 주세요.")
                .build();
    }
}