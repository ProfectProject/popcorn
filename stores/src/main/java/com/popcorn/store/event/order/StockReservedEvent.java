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
 * 재고 예약 성공 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StockReservedEvent {

    private String eventId;
    private UUID orderId;
    private String orderNo;
    private UUID popupId;
    private Long customerId;
    private List<ReservedStockItem> reservedItems;
    private LocalDateTime reservationExpiresAt;
    private LocalDateTime eventTime;

    public static StockReservedEvent create(UUID orderId, String orderNo, UUID popupId,
                                            Long customerId, List<ReservedStockItem> reservedItems) {
        return StockReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .popupId(popupId)
                .customerId(customerId)
                .reservedItems(reservedItems)
                .reservationExpiresAt(LocalDateTime.now().plusMinutes(30))
                .eventTime(LocalDateTime.now())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class ReservedStockItem {
        private UUID goodsId;
        private UUID scheduleId;
        private Integer quantity;
        private Integer unitPrice;
        private String productName;
        private String reservationName;
        private Integer reservationDetails;
        private ReservationCategory reservationCategory;

        public static ReservedStockItem goods(UUID goodsId, Integer quantity,
                                              Integer unitPrice, String productName) {
            return ReservedStockItem.builder()
                    .goodsId(goodsId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .productName(productName)
                    .reservationCategory(ReservationCategory.GOODS)
                    .build();
        }

        public static ReservedStockItem schedule(UUID scheduleId, Integer quantity,
                                                 Integer unitPrice, String reservationName,
                                                 Integer reservationDetails) {
            return ReservedStockItem.builder()
                    .scheduleId(scheduleId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .reservationName(reservationName)
                    .reservationDetails(reservationDetails)
                    .reservationCategory(ReservationCategory.SCHEDULE)
                    .build();
        }

        public enum ReservationCategory {
            GOODS,
            SCHEDULE
        }
    }
}
