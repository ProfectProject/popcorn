package com.popcorn.order.event.standard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 표준 주문 생성 이벤트
 * 새로운 주문이 생성되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardOrderCreatedEvent extends StandardBaseEvent {

    /**
     * 주문 일시
     */
    private LocalDateTime orderedAt;

    /**
     * 주문 상태
     */
    private String orderStatus;

    /**
     * 총 주문 금액
     */
    private Integer totalAmount;

    /**
     * 정적 팩토리 메서드 - Order 도메인 객체로부터 생성
     */
    public static StandardOrderCreatedEvent from(Object order) {
        StandardOrderCreatedEvent event = StandardOrderCreatedEvent.builder()
                .eventType(StandardEventType.ORDER_CREATED)
                .producer("order-service")
                .orderStatus("REQUESTED")
                .build();

        // TODO: Order 엔티티에서 실제 값 매핑
        event.setDefaults();
        return event;
    }

    /**
     * Redis Stream 발행용 Map 변환
     */
    public java.util.Map<String, String> toStreamMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();

        // 공통 필드
        map.put("eventId", this.getEventId() != null ? this.getEventId() : "");
        map.put("eventType", this.getEventType() != null ? this.getEventType().getValue() : "");
        map.put("occurredAt", this.getOccurredAt() != null ? this.getOccurredAt().toString() : "");
        map.put("producer", this.getProducer() != null ? this.getProducer() : "");

        // 비즈니스 필드
        map.put("orderId", this.getOrderId() != null ? this.getOrderId().toString() : "");
        map.put("orderNo", this.getOrderNo() != null ? this.getOrderNo() : "");
        map.put("userId", this.getUserId() != null ? this.getUserId().toString() : "");
        map.put("storeId", this.getStoreId() != null ? this.getStoreId().toString() : "");
        map.put("popupId", this.getPopupId() != null ? this.getPopupId().toString() : "");
        map.put("orderedAt", this.getOrderedAt() != null ? this.getOrderedAt().toString() : "");
        map.put("orderStatus", this.getOrderStatus() != null ? this.getOrderStatus() : "");
        map.put("totalAmount", this.getTotalAmount() != null ? this.getTotalAmount().toString() : "");
        map.put("hasReservation", this.getHasReservation() != null ? this.getHasReservation().toString() : "false");
        map.put("hasGoods", this.getHasGoods() != null ? this.getHasGoods().toString() : "false");

        // lines는 JSON으로 직렬화 필요 (별도 처리)
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