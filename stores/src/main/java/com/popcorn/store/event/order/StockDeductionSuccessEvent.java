package com.popcorn.store.event.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 재고 차감 성공 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StockDeductionSuccessEvent {

    private String eventId;
    private UUID orderId;
    private String orderNo;
    private UUID popupId;
    private String stockDetails;
    private LocalDateTime succeededAt;
    private LocalDateTime eventTime;

    public static StockDeductionSuccessEvent create(UUID orderId, String orderNo,
                                                    UUID popupId, String stockDetails) {
        return StockDeductionSuccessEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .popupId(popupId)
                .stockDetails(stockDetails)
                .succeededAt(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .build();
    }
}
