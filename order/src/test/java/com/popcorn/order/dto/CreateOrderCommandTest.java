package com.popcorn.order.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.order.dto.command.CreateOrderCommand;
import com.popcorn.order.dto.request.CreateOrderRequest;
import com.popcorn.order.dto.request.OrderItemRequest;

/**
 * 자바 초보자용 CreateOrderCommand 테스트
 */
class CreateOrderCommandTest {

    @Test
    void Command_객체_생성_테스트() {
        // Given
        Long userId = 1L;
        UUID popupId = UUID.randomUUID();
        String orderType = "RESERVATION";

        // When
        CreateOrderCommand command = CreateOrderCommand.builder()
                .userId(userId)
                .popupId(popupId)
                .orderType(orderType)
                .build();

        // Then
        assertEquals(userId, command.getUserId());
        assertEquals(popupId, command.getPopupId());
        assertEquals(orderType, command.getOrderType());
    }

    @Test
    void Request에서_Command로_변환_테스트() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .popupId(UUID.randomUUID())
                .orderType("RESERVATION")
                .items(List.of(
                        OrderItemRequest.builder()
                                .orderItemType("RESERVATION")
                                .sessionId(UUID.randomUUID())
                                .qty(1)
                                .build()
                ))
                .build();

        // When
        CreateOrderCommand command = CreateOrderCommand.fromRequest(request);

        // Then
        assertEquals(request.getUserId(), command.getUserId());
        assertEquals(request.getPopupId(), command.getPopupId());
        assertEquals(request.getOrderType(), command.getOrderType());
        assertEquals(1, command.getItems().size());
    }

}