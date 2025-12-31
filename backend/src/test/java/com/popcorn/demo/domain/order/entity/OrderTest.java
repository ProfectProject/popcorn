package com.popcorn.demo.domain.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    @DisplayName("Order number format includes prefix and date")
    void orderNumberFormat() {
        String orderNo = Order.generateOrderNo();

        assertThat(orderNo).startsWith("O");
        assertThat(orderNo).contains("-");
        assertThat(orderNo).hasSize(16);
    }

    @Test
    @DisplayName("Type checks and cancelable logic")
    void typeChecksAndCancelableLogic() {
        Order reservationOrder = Order.builder()
                .orderNo("O-1")
                .customerId(1L)
                .storeId(1L)
                .productId(1L)
                .orderType(OrderType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .totalAmount(10000)
                .cancelableUntil(LocalDateTime.now().plusMinutes(10))
                .build();

        Order purchaseOrder = Order.builder()
                .orderNo("O-2")
                .customerId(1L)
                .storeId(1L)
                .productId(1L)
                .orderType(OrderType.PURCHASE)
                .status(OrderStatus.REQUESTED)
                .totalAmount(10000)
                .cancelableUntil(LocalDateTime.now().minusMinutes(10))
                .build();

        assertThat(reservationOrder.isReservationType()).isTrue();
        assertThat(reservationOrder.isPurchaseType()).isFalse();
        assertThat(reservationOrder.isCancelable()).isTrue();

        assertThat(purchaseOrder.isReservationType()).isFalse();
        assertThat(purchaseOrder.isPurchaseType()).isTrue();
        assertThat(purchaseOrder.isCancelable()).isFalse();
    }

    @Test
    @DisplayName("Total quantity sums item quantities")
    void totalQuantitySumsItems() {
        Order order = Order.builder()
                .orderNo("O-3")
                .customerId(1L)
                .storeId(1L)
                .productId(1L)
                .orderType(OrderType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .totalAmount(10000)
                .build();

        OrderItem item1 = OrderItem.builder()
                .order(order)
                .orderItemType(OrderItemType.RESERVATION)
                .sessionOptionId(10L)
                .qty(2)
                .unitPrice(1000)
                .lineAmount(2000)
                .build();

        OrderItem item2 = OrderItem.builder()
                .order(order)
                .orderItemType(OrderItemType.RESERVATION)
                .sessionOptionId(11L)
                .qty(3)
                .unitPrice(1000)
                .lineAmount(3000)
                .build();

        order.setOrderItems(List.of(item1, item2));

        assertThat(order.getTotalQuantity()).isEqualTo(5);
    }
}
