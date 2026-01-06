package com.popcorn.demo.domain.order.event;

import java.util.Map;
import java.util.UUID;

import com.popcorn.demo.domain.order.entity.OrderStatus;

import lombok.Getter;

/**
 * 주문 취소 이벤트
 *
 * 주문이 취소되었을 때 발생하는 이벤트
 * 취소 사유 추적 및 보상 처리에 활용
 */
@Getter
public class OrderCancelledEvent extends BaseOrderEvent {

    private final OrderStatus previousStatus;
    private final String cancellationReason;
    private final String cancelledBy;        // 취소 주체 (고객/점주/시스템)
    private final String cancellationType;   // CUSTOMER, OWNER, SYSTEM, AUTO
    private final Integer refundAmount;      // 환불 예정 금액
    private final boolean isRefundRequired;  // 환불 필요 여부

    public OrderCancelledEvent(
            UUID orderId,
            Long userId,
            OrderStatus previousStatus,
            String cancellationReason,
            String cancelledBy,
            Integer refundAmount) {

        super(
            orderId,
            "order_cancelled",
            userId,
            Map.of(
                "previousStatus", previousStatus.name(),
                "cancellationReason", cancellationReason != null ? cancellationReason : "사유 없음",
                "cancelledBy", cancelledBy != null ? cancelledBy : "SYSTEM",
                "cancellationType", determineCancellationType(cancelledBy),
                "refundAmount", refundAmount != null ? refundAmount : 0,
                "isRefundRequired", refundAmount != null && refundAmount > 0,
                "isEarlyCancellation", isEarlyCancellation(previousStatus),
                "requiresCompensation", requiresCompensation(previousStatus)
            )
        );

        this.previousStatus = previousStatus;
        this.cancellationReason = cancellationReason;
        this.cancelledBy = cancelledBy != null ? cancelledBy : "SYSTEM";
        this.cancellationType = determineCancellationType(cancelledBy);
        this.refundAmount = refundAmount != null ? refundAmount : 0;
        this.isRefundRequired = this.refundAmount > 0;
    }

    // ================ Business Logic ================

    /**
     * 조기 취소 여부 (결제 전 취소)
     */
    public boolean isEarlyCancellation() {
        return isEarlyCancellation(previousStatus);
    }

    /**
     * 보상이 필요한 취소인지 확인
     */
    public boolean requiresCompensation() {
        return requiresCompensation(previousStatus);
    }

    /**
     * 고객 주도 취소인지 확인
     */
    public boolean isCustomerInitiated() {
        return "CUSTOMER".equals(cancellationType);
    }

    /**
     * 점주 주도 취소인지 확인
     */
    public boolean isOwnerInitiated() {
        return "OWNER".equals(cancellationType);
    }

    /**
     * 시스템 자동 취소인지 확인
     */
    public boolean isSystemCancellation() {
        return "SYSTEM".equals(cancellationType) || "AUTO".equals(cancellationType);
    }

    /**
     * 취소 심각도 평가
     */
    public String getCancellationSeverity() {
        if (previousStatus == OrderStatus.PAID) {
            return "HIGH";  // 결제 완료 후 취소
        }
        if (previousStatus == OrderStatus.RESERVED || previousStatus == OrderStatus.PAYMENT_PENDING) {
            return "MEDIUM"; // 예약/결제 대기 단계 취소
        }
        return "LOW";  // 초기 단계 취소
    }

    // ================ Event Payload ================

    @Override
    protected Map<String, Object> getEventPayload() {
        return Map.of(
            "orderId", getOrderId(),
            "userId", getUserId(),
            "previousStatus", previousStatus.name(),
            "cancellationReason", cancellationReason != null ? cancellationReason : "",
            "cancelledBy", cancelledBy,
            "cancellationType", cancellationType,
            "refundAmount", refundAmount,
            "isRefundRequired", isRefundRequired,
            "isEarlyCancellation", isEarlyCancellation(),
            "severity", getCancellationSeverity()
        );
    }

    /**
     * 취소 상세 설명
     */
    public String getCancellationDescription() {
        String refundText = isRefundRequired
            ? String.format(" (환불: %,d원)", refundAmount)
            : "";

        return String.format(
            "주문 취소됨: [이전상태=%s, 사유=%s, 취소자=%s%s]",
            getStatusDisplayName(previousStatus),
            cancellationReason != null ? cancellationReason : "사유없음",
            cancelledBy,
            refundText
        );
    }

    // ================ Helper Methods ================

    private static String determineCancellationType(String cancelledBy) {
        if (cancelledBy == null || "SYSTEM".equals(cancelledBy)) {
            return "SYSTEM";
        }
        if (cancelledBy.matches("\\d+")) {
            return "CUSTOMER";  // 숫자는 고객 ID로 가정
        }
        if (cancelledBy.contains("OWNER") || cancelledBy.contains("STORE")) {
            return "OWNER";
        }
        if (cancelledBy.contains("AUTO") || cancelledBy.contains("TIMEOUT")) {
            return "AUTO";
        }
        return "SYSTEM";
    }

    private static boolean isEarlyCancellation(OrderStatus status) {
        return status == OrderStatus.REQUESTED || status == OrderStatus.ACCEPTED;
    }

    private static boolean requiresCompensation(OrderStatus status) {
        return status == OrderStatus.PAID;
    }

    private String getStatusDisplayName(OrderStatus status) {
        return switch (status) {
            case REQUESTED -> "요청됨";
            case ACCEPTED -> "승인됨";
            case RESERVED -> "예약됨";
            case PAYMENT_PENDING -> "결제대기";
            case PAID -> "결제완료";
            default -> status.name();
        };
    }

    @Override
    public String toString() {
        return String.format("OrderCancelledEvent[orderId=%s, from=%s, by=%s, refund=%d]",
                getOrderId(), previousStatus, cancelledBy, refundAmount);
    }
}
