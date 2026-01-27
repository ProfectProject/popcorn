package com.popcorn.store.event.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 재고 예약 실패 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StockReservationFailedEvent {

    private String eventId;
    private UUID orderId;
    private String orderNo;
    private UUID popupId;
    private Long customerId;
    private List<FailedStockItem> failedItems;
    private String failureReason;
    private LocalDateTime eventTime;

    public static StockReservationFailedEvent create(UUID orderId, String orderNo, UUID popupId,
                                                    Long customerId, List<FailedStockItem> failedItems,
                                                    String failureReason) {
        return StockReservationFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .popupId(popupId)
                .customerId(customerId)
                .failedItems(failedItems)
                .failureReason(failureReason)
                .eventTime(LocalDateTime.now())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class FailedStockItem {
        private UUID goodsId;
        private Integer requestedQuantity;
        private Integer availableQuantity;
        private String productName;
        private String failureReason;

        public static FailedStockItem create(UUID goodsId, Integer requestedQuantity,
                                             Integer availableQuantity, String productName,
                                             String failureReason) {
            return FailedStockItem.builder()
                    .goodsId(goodsId)
                    .requestedQuantity(requestedQuantity)
                    .availableQuantity(availableQuantity)
                    .productName(productName)
                    .failureReason(failureReason)
                    .build();
        }
    }
}
