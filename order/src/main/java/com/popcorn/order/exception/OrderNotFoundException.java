package com.popcorn.order.exception;

import java.util.UUID;

import lombok.Getter;

/**
 * 주문을 찾을 수 없을 때 발생하는 예외
 *
 * 사용자가 존재하지 않는 주문 ID로 조회하거나 수정을 시도할 때 발생합니다.
 * 보안상 과도한 정보를 노출하지 않도록 주의해야 합니다.
 */
@Getter
public class OrderNotFoundException extends RuntimeException {

    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super(String.format("주문을 찾을 수 없습니다. 주문ID: %s", orderId));
        this.orderId = orderId;
    }

    public OrderNotFoundException(UUID orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

}