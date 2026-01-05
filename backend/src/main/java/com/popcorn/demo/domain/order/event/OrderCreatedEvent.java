package com.popcorn.demo.domain.order.event;

import java.util.Map;
import java.util.UUID;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;

import lombok.Getter;

/**
 * 주문 생성 이벤트
 *
 * 주문이 성공적으로 생성되었을 때 발생하는 이벤트
 */
@Getter
public class OrderCreatedEvent extends BaseOrderEvent {

    private final Order order;
    private final String idempotencyKey;
    private final Integer totalAmount;
    private final Integer itemCount;
    private final UUID storeId;

    public OrderCreatedEvent(Order order, String idempotencyKey) {
        super(
            order.getId(),
            "order_created",
            order.getCustomerId(),
            Map.of(
                "orderType", order.getOrderType().name(),
                "status", order.getStatus().name(),
                "storeId", order.getStoreId().toString(),
                "totalAmount", order.getTotalAmount(),
                "itemCount", order.getOrderItems().size(),
                "idempotencyKey", idempotencyKey != null ? idempotencyKey : ""
            )
        );

        this.order = order;
        this.idempotencyKey = idempotencyKey;
        this.totalAmount = order.getTotalAmount();
        this.itemCount = order.getOrderItems().size();
        this.storeId = order.getStoreId();
    }

    public OrderType getOrderType() { return order.getOrderType(); }
    public OrderStatus getOrderStatus() { return order.getStatus(); }

    // ================ Event Payload ================

    @Override
    protected Map<String, Object> getEventPayload() {
        return Map.of(
            "orderId", getOrderId(),
            "userId", getUserId(),
            "storeId", storeId,
            "orderType", order.getOrderType().name(),
            "status", order.getStatus().name(),
            "totalAmount", totalAmount,
            "itemCount", itemCount,
            "idempotencyKey", idempotencyKey != null ? idempotencyKey : "",
            "createdAt", order.getCreatedAt()
        );
    }

    /**
     * 이벤트 상세 설명
     */
    public String getDetailedDescription() {
        return String.format(
            "주문 생성됨: [주문ID=%s, 사용자=%s, 상점=%s, 타입=%s, 금액=%d원, 아이템=%d개]",
            getOrderId(), getUserId(), storeId, order.getOrderType().name(),
            totalAmount, itemCount
        );
    }

    /**
     * 비즈니스 중요도 반환 (모니터링용)
     */
    public String getBusinessPriority() {
        // 금액에 따른 중요도 분류
        if (totalAmount >= 100000) return "HIGH";
        if (totalAmount >= 50000) return "MEDIUM";
        return "LOW";
    }
}
