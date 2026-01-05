package com.popcorn.demo.domain.order.event;

import java.util.Map;
import java.util.UUID;

import com.popcorn.demo.domain.order.entity.OrderStatus;

/**
 * 주문 상태 변경 이벤트
 *
 * 주문 상태가 변경되었을 때 발생하는 이벤트
 * 상태 전이 추적 및 비즈니스 로직 트리거에 활용
 */
public class OrderStatusChangedEvent extends BaseOrderEvent {

    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;
    private final String reason;
    private final String changedBy;    // 변경한 사용자/시스템
    private final String changeType;   // MANUAL, AUTOMATIC, SYSTEM

    public OrderStatusChangedEvent(
            UUID orderId,
            Long userId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            String changedBy) {

        super(
            orderId,
            "order_status_changed",
            userId,
            Map.of(
                "fromStatus", fromStatus.name(),
                "toStatus", toStatus.name(),
                "reason", reason != null ? reason : "",
                "changedBy", changedBy != null ? changedBy : "SYSTEM",
                "changeType", determineChangeType(changedBy),
                "isStatusProgression", isStatusProgression(fromStatus, toStatus),
                "isReversible", isReversibleChange(fromStatus, toStatus)
            )
        );

        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedBy = changedBy != null ? changedBy : "SYSTEM";
        this.changeType = determineChangeType(changedBy);
    }

    // ================ Getters ================

    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public String getChangedBy() { return changedBy; }
    public String getChangeType() { return changeType; }

    // ================ Business Logic ================

    /**
     * 상태 진행 여부 확인 (정방향 진행인지)
     */
    public boolean isStatusProgression() {
        return isStatusProgression(fromStatus, toStatus);
    }

    /**
     * 되돌릴 수 있는 변경인지 확인
     */
    public boolean isReversibleChange() {
        return isReversibleChange(fromStatus, toStatus);
    }

    /**
     * 중요한 상태 변경인지 확인 (알림 필요 등)
     */
    public boolean isCriticalStatusChange() {
        return toStatus == OrderStatus.COMPLETED ||
               toStatus == OrderStatus.CANCELLED ||
               toStatus == OrderStatus.OWNER_REJECTED ||
               (fromStatus == OrderStatus.REQUESTED && toStatus == OrderStatus.OWNER_ACCEPTED);
    }

    /**
     * 고객 알림이 필요한 상태 변경인지 확인
     */
    public boolean requiresCustomerNotification() {
        return toStatus == OrderStatus.OWNER_ACCEPTED ||
               toStatus == OrderStatus.OWNER_REJECTED ||
               toStatus == OrderStatus.READY ||
               toStatus == OrderStatus.COMPLETED ||
               toStatus == OrderStatus.CANCELLED;
    }

    // ================ Event Payload ================

    @Override
    protected Map<String, Object> getEventPayload() {
        return Map.of(
            "orderId", getOrderId(),
            "userId", getUserId(),
            "fromStatus", fromStatus.name(),
            "toStatus", toStatus.name(),
            "reason", reason != null ? reason : "",
            "changedBy", changedBy,
            "changeType", changeType,
            "isProgression", isStatusProgression(),
            "isCritical", isCriticalStatusChange(),
            "requiresNotification", requiresCustomerNotification()
        );
    }

    /**
     * 상태 변경 요약 설명
     */
    public String getStatusChangeDescription() {
        String reasonText = reason != null && !reason.trim().isEmpty()
            ? String.format(" (사유: %s)", reason)
            : "";

        return String.format("%s → %s%s [변경자: %s]",
            getStatusDisplayName(fromStatus),
            getStatusDisplayName(toStatus),
            reasonText,
            changedBy);
    }

    // ================ Helper Methods ================

    private static String determineChangeType(String changedBy) {
        if (changedBy == null || "SYSTEM".equals(changedBy)) {
            return "SYSTEM";
        }
        return changedBy.matches("\\d+") ? "MANUAL" : "AUTOMATIC";
    }

    private static boolean isStatusProgression(OrderStatus from, OrderStatus to) {
        // 정방향 진행 판단 로직
        return switch (from) {
            case REQUESTED -> to == OrderStatus.OWNER_ACCEPTED || to == OrderStatus.CONFIRMED;
            case OWNER_ACCEPTED -> to == OrderStatus.CONFIRMED || to == OrderStatus.PREPARING;
            case CONFIRMED -> to == OrderStatus.PREPARING;
            case PREPARING -> to == OrderStatus.READY;
            case READY -> to == OrderStatus.COMPLETED;
            default -> false;
        };
    }

    private static boolean isReversibleChange(OrderStatus from, OrderStatus to) {
        // 되돌릴 수 있는 변경 판단
        return !(to == OrderStatus.COMPLETED ||
                to == OrderStatus.CANCELLED ||
                to == OrderStatus.REFUNDED ||
                to == OrderStatus.OWNER_REJECTED);
    }

    private String getStatusDisplayName(OrderStatus status) {
        return switch (status) {
            case REQUESTED -> "요청됨";
            case OWNER_ACCEPTED -> "점주승인";
            case OWNER_REJECTED -> "점주거절";
            case CONFIRMED -> "확인됨";
            case PREPARING -> "준비중";
            case READY -> "준비완료";
            case COMPLETED -> "완료";
            case CANCELLED -> "취소됨";
            case REFUNDED -> "환불됨";
        };
    }

    @Override
    public String toString() {
        return String.format("OrderStatusChangedEvent[orderId=%s, %s → %s, changedBy=%s]",
                getOrderId(), fromStatus, toStatus, changedBy);
    }
}