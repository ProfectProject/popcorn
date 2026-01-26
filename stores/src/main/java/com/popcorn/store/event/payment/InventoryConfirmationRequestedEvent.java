package com.popcorn.store.event.payment;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class InventoryConfirmationRequestedEvent {

    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_RESTORE = "RESTORE";

    private final UUID paymentId;
    private final UUID orderId;
    private final String actionType;
    private final String reason;
    private final LocalDateTime requestedAt;
    private final LocalDateTime occurredAt;
    private final String eventId;

    private InventoryConfirmationRequestedEvent(UUID paymentId, UUID orderId, String actionType,
                                                String reason, LocalDateTime requestedAt,
                                                LocalDateTime occurredAt, String eventId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.actionType = actionType;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.occurredAt = occurredAt;
        this.eventId = eventId;
    }

    public static InventoryConfirmationRequestedEvent createConfirm(UUID paymentId, UUID orderId) {
        return new InventoryConfirmationRequestedEvent(
                paymentId,
                orderId,
                ACTION_CONFIRM,
                "결제 승인 완료",
                LocalDateTime.now(),
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }

    public static InventoryConfirmationRequestedEvent createRestore(UUID paymentId, UUID orderId) {
        return new InventoryConfirmationRequestedEvent(
                paymentId,
                orderId,
                ACTION_RESTORE,
                "결제 실패/취소",
                LocalDateTime.now(),
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }

    public boolean isConfirmAction() {
        return ACTION_CONFIRM.equals(actionType);
    }

    public boolean isRestoreAction() {
        return ACTION_RESTORE.equals(actionType);
    }
}
