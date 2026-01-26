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
 * 주문 결제 완료 이벤트 (Store 서비스에서 구독)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderPaidEvent {

    private String eventId;
    private UUID orderId;
    private String orderNo;
    private Long customerId;
    private UUID popupId;
    private String orderType;
    private Integer totalAmount;
    private List<OrderItemInfo> orderItems;
    private LocalDateTime paidAt;
    private LocalDateTime eventTime;

    public List<OrderItemInfo> getGoodsItems() {
        return orderItems == null ? List.of() :
                orderItems.stream().filter(OrderItemInfo::isGoodsItem).toList();
    }

    public List<OrderItemInfo> getReservationItems() {
        return orderItems == null ? List.of() :
                orderItems.stream().filter(OrderItemInfo::isReservationItem).toList();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class OrderItemInfo {

        private String orderItemType;
        private Integer quantity;
        private UUID sessionId;
        private UUID goodsVariantId;
        private Integer unitPrice;

        public boolean isGoodsItem() {
            return "GOODS".equals(orderItemType);
        }

        public boolean isReservationItem() {
            return "RESERVATION".equals(orderItemType);
        }
    }
}
