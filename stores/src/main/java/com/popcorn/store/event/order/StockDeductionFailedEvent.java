package com.popcorn.store.event.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 재고 차감 실패 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class StockDeductionFailedEvent {

    private String eventId;
    private UUID orderId;
    private String orderNo;
    private UUID popupId;
    private String reason;
    private String details;
    private String failureCode;
    private LocalDateTime failedAt;
    private LocalDateTime eventTime;

    public static StockDeductionFailedEvent create(UUID orderId, String orderNo,
                                                   UUID popupId, String reason,
                                                   String failureCode, String details) {
        return StockDeductionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .popupId(popupId)
                .reason(reason)
                .failureCode(failureCode)
                .details(details)
                .failedAt(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .build();
    }

    public static StockDeductionFailedEvent forInsufficientStock(UUID orderId, String orderNo,
                                                                 UUID popupId, String details) {
        return create(orderId, orderNo, popupId, "재고 부족", "INSUFFICIENT_STOCK", details);
    }

    public static StockDeductionFailedEvent forSystemError(UUID orderId, String orderNo,
                                                           UUID popupId, String details) {
        return create(orderId, orderNo, popupId, "시스템 오류", "SYSTEM_ERROR", details);
    }
}
