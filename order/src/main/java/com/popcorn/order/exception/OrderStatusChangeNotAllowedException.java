package com.popcorn.order.exception;

import java.util.UUID;

import lombok.Getter;

/**
 * 주문 상태 변경이 허용되지 않을 때 발생하는 예외
 *
 * 비즈니스 규칙에 따라 특정 상태에서 다른 상태로의 변경이 금지된 경우 발생합니다.
 * 예: 이미 배송 시작된 주문을 대기 상태로 변경하려는 경우
 */
@Getter
public class OrderStatusChangeNotAllowedException extends RuntimeException {

    private final UUID orderId;
    private final String currentStatus;
    private final String requestedStatus;

    public OrderStatusChangeNotAllowedException(UUID orderId, String currentStatus, String requestedStatus) {
        super(String.format("주문 상태 변경이 불가능합니다. 주문ID: %s, 현재상태: %s, 요청상태: %s",
                orderId, currentStatus, requestedStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public OrderStatusChangeNotAllowedException(UUID orderId, String currentStatus, String requestedStatus, String reason) {
        super(reason);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

}