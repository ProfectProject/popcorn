package com.popcorn.order.exception;

import java.util.UUID;

import lombok.Getter;

/**
 * 주문 취소가 불가능할 때 발생하는 예외
 *
 * 이미 배송이 시작되었거나 완료된 주문을 취소하려 할 때 발생합니다.
 * 취소 정책에 따른 명확한 안내를 제공합니다.
 */
@Getter
public class OrderNotCancellableException extends RuntimeException {

    private final UUID orderId;
    private final String currentStatus;

    public OrderNotCancellableException(UUID orderId, String currentStatus) {
        super(String.format("주문을 취소할 수 없습니다. 주문ID: %s, 현재상태: %s",
                orderId, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public OrderNotCancellableException(UUID orderId, String currentStatus, String reason) {
        super(reason);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

}