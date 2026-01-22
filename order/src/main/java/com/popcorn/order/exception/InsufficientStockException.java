package com.popcorn.order.exception;

import java.util.UUID;

import lombok.Getter;

/**
 * 재고 부족으로 주문을 처리할 수 없을 때 발생하는 예외
 *
 * 주문 요청 수량이 현재 재고보다 많을 때 발생합니다.
 * 사용자에게 현재 재고 상황을 명확히 안내합니다.
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final UUID productId;
    private final Integer requestedQuantity;
    private final Integer availableStock;

    public InsufficientStockException(UUID productId, Integer requestedQuantity, Integer availableStock) {
        super(String.format("재고가 부족합니다. 상품ID: %s, 요청수량: %d, 재고수량: %d",
                productId, requestedQuantity, availableStock));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }

    public InsufficientStockException(UUID productId, Integer requestedQuantity, Integer availableStock, String message) {
        super(message);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }

}