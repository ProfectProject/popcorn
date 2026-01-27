package com.popcorn.order.event.standard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 표준 주문 상태 변경 이벤트
 * 주문 상태가 변경되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardOrderStatusUpdatedEvent extends StandardBaseEvent {

    /**
     * 변경 전 상태
     */
    private String fromStatus;

    /**
     * 변경 후 상태
     */
    private String toStatus;

    /**
     * 정적 팩토리 메서드
     */
    public static StandardOrderStatusUpdatedEvent create(String fromStatus, String toStatus) {
        StandardOrderStatusUpdatedEvent event = StandardOrderStatusUpdatedEvent.builder()
                .eventType(StandardEventType.ORDER_STATUS_UPDATED)
                .producer("order-service")
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .build();

        event.setDefaults();
        return event;
    }

    /**
     * Redis Stream 발행용 Map 변환
     */
    public java.util.Map<String, String> toStreamMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();

        // 공통 필드
        map.put("eventId", this.getEventId());
        map.put("eventType", this.getEventType().getValue());
        map.put("occurredAt", this.getOccurredAt().toString());
        map.put("producer", this.getProducer());

        // 비즈니스 필드
        map.put("orderId", this.getOrderId() != null ? this.getOrderId().toString() : "");
        map.put("storeId", this.getStoreId() != null ? this.getStoreId().toString() : "");
        map.put("popupId", this.getPopupId() != null ? this.getPopupId().toString() : "");
        map.put("fromStatus", this.getFromStatus() != null ? this.getFromStatus() : "");
        map.put("toStatus", this.getToStatus() != null ? this.getToStatus() : "");
        map.put("hasReservation", this.getHasReservation() != null ? this.getHasReservation().toString() : "false");
        map.put("hasGoods", this.getHasGoods() != null ? this.getHasGoods().toString() : "false");

        // lines JSON 직렬화
        if (this.getLines() != null && !this.getLines().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                map.put("lines", objectMapper.writeValueAsString(this.getLines()));
            } catch (Exception e) {
                map.put("lines", "[]");
            }
        } else {
            map.put("lines", "[]");
        }

        return map;
    }
}