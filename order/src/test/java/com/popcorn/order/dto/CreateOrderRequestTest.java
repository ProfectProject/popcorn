package com.popcorn.order.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.popcorn.order.dto.request.CreateOrderRequest;
import com.popcorn.order.dto.request.OrderItemRequest;

/**
 * CreateOrderRequest 테스트
 *
 * 자바 초보자용: 아주 쉬운 테스트 예제
 * - getter/setter 테스트
 * - 객체 생성 테스트
 * - 값 비교 테스트
 */
class CreateOrderRequestTest {

    @Test
    void 주문요청_생성_테스트() {
        // Given (준비) - 테스트에 필요한 데이터 만들기
        Long userId = 1L;
        UUID popupId = UUID.randomUUID();
        String orderType = "RESERVATION";

        // When (실행) - 실제 테스트할 코드 실행
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(userId)
                .popupId(popupId)
                .orderType(orderType)
                .build();

        // Then (검증) - 결과가 올바른지 확인
        assertEquals(userId, request.getUserId());
        assertEquals(popupId, request.getPopupId());
        assertEquals(orderType, request.getOrderType());
    }

    @Test
    void 예약타입_확인_테스트() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("RESERVATION")
                .build();

        // When & Then
        assertTrue(request.isReservationType());
        assertFalse(request.isPurchaseType());
    }

    @Test
    void 구매타입_확인_테스트() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("PURCHASE")
                .build();

        // When & Then
        assertTrue(request.isPurchaseType());
        assertFalse(request.isReservationType());
    }

    @Test
    void 주문아이템_포함_테스트() {
        // Given
        OrderItemRequest item = OrderItemRequest.builder()
                .orderItemType("RESERVATION")
                .sessionId(UUID.randomUUID())
                .qty(2)
                .build();

        // When
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(item))
                .build();

        // Then
        assertNotNull(request.getItems());
        assertEquals(1, request.getItems().size());
        assertEquals("RESERVATION", request.getItems().get(0).getOrderItemType());
    }

    @Test
    void 총수량_계산_테스트() {
        // Given
        OrderItemRequest item1 = OrderItemRequest.builder().qty(3).build();
        OrderItemRequest item2 = OrderItemRequest.builder().qty(5).build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(item1, item2))
                .build();

        // When & Then
        assertEquals(8, request.getTotalQuantity());
    }

}