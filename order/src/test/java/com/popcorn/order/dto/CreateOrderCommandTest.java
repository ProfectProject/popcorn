package com.popcorn.order.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.order.dto.command.CreateOrderCommand;
import com.popcorn.order.dto.request.OrderCreateRequest;
import com.popcorn.order.dto.request.OrderItemRequest;

/**
 * CreateOrderCommand 테스트 (MSA 구조로 업데이트됨)
 *
 * [Java 초보자를 위한 가이드]
 *
 * MSA 구조 변경사항:
 * 1. CreateOrderRequest에서 userId 제거됨 (JWT에서 추출)
 * 2. fromRequest 메소드가 userId 파라미터를 별도로 받음
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
        Long userId = 1L; // MSA에서는 JWT에서 추출된 사용자 ID
        OrderCreateRequest request = OrderCreateRequest.builder()
                // userId는 request에서 제거됨 (JWT에서 추출)
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
        CreateOrderCommand command = CreateOrderCommand.fromRequest(request, userId); // userId 별도 전달

        // Then
        assertEquals(userId, command.getUserId()); // 직접 전달한 userId 확인
        assertEquals(request.getPopupId(), command.getPopupId());
        assertEquals(request.getOrderType(), command.getOrderType());
        assertEquals(1, command.getItems().size());
    }

}
