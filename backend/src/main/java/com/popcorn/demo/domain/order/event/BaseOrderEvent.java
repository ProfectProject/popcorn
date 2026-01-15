package com.popcorn.demo.domain.order.event;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.popcorn.demo.common.event.BaseEvent;

import lombok.Getter;

/**
 * 주문 도메인 이벤트의 기본 클래스
 *
 * 기능:
 * - BaseEvent를 상속받아 공통 이벤트 기능 활용
 * - 주문 도메인 특화 기능 제공
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "order_created"),
    @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "order_status_changed"),
    @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "order_cancelled"),
    @JsonSubTypes.Type(value = OrderCompletedEvent.class, name = "order_completed"),
    @JsonSubTypes.Type(value = OrderPaymentProcessedEvent.class, name = "order_payment_processed")
})
@Getter
public abstract class BaseOrderEvent extends BaseEvent {

    protected BaseOrderEvent(UUID orderId, String eventType, Long userId, Map<String, Object> metadata) {
        super(orderId, "Order", eventType, userId, metadata);
    }

    protected BaseOrderEvent(UUID orderId, String eventType, Long userId) {
        this(orderId, eventType, userId, null);
    }

    // ================ Order 도메인 특화 메서드 ================

    /**
     * 주문 ID 조회 (getAggregateId()의 별칭)
     */
    public UUID getOrderId() {
        return getAggregateId();
    }
}
