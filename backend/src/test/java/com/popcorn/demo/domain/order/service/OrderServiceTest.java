package com.popcorn.demo.domain.order.service;

import com.popcorn.demo.domain.order.dto.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.AddressRequest;
import com.popcorn.demo.domain.order.dto.OrderCreatedDto;
import com.popcorn.demo.domain.order.dto.OrderItemRequest;
import com.popcorn.demo.domain.order.entity.*;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrderService 테스트 - TDD 방식
 *
 * 테스트 시나리오:
 * 1. 예약형 주문 생성 성공 테스트
 * 2. 구매형 주문 생성 성공 테스트
 * 3. 주문 아이템 없음 예외 테스트
 * 4. 비동기 후처리 작업 호출 검증
 * 5. DI 주입된 의존성 동작 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 서비스 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderAsyncService orderAsyncService;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest reservationRequest;
    private CreateOrderRequest purchaseRequest;
    private Order mockSavedOrder;

    @BeforeEach
    @DisplayName("테스트 데이터 준비")
    void setUp() {
        lenient().when(orderAsyncService.validateOrderAsync(anyLong(), anyLong(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(true));
        lenient().when(orderAsyncService.processOrderPostActions(anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // 예약형 주문 요청 데이터
        OrderItemRequest reservationItem = OrderItemRequest.builder()
                .orderItemType("RESERVATION")
                .sessionId(101L)
                .qty(2)
                .build();

        reservationRequest = CreateOrderRequest.builder()
                .storeId(1L)
                .productId(2L)
                .orderType("RESERVATION")
                .items(Arrays.asList(reservationItem))
                .build();

        // 구매형 주문 요청 데이터 (주소 정보 포함)
        OrderItemRequest merchItem = OrderItemRequest.builder()
                .orderItemType("MERCH")
                .merchVariantId(201L)
                .qty(1)
                .build();

        AddressRequest address = AddressRequest.builder()
                .address1("서울시 강남구 테헤란로 123")
                .address2("456호")
                .receiverName("홍길동")
                .phone("010-1234-5678")
                .build();

        purchaseRequest = CreateOrderRequest.builder()
                .storeId(1L)
                .productId(3L)
                .orderType("PURCHASE")
                .address(address)
                .items(Arrays.asList(merchItem))
                .build();

        // Mock 저장된 주문 데이터
        mockSavedOrder = Order.builder()
                .orderNo("O20251230-000001")
                .customerId(1001L)
                .storeId(1L)
                .productId(2L)
                .orderType(OrderType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .totalAmount(29000)
                .cancelableUntil(LocalDateTime.now().plusHours(1))
                .build();
        mockSavedOrder.setId(1L);
        OrderItem mockItem = OrderItem.builder()
                .order(mockSavedOrder)
                .orderItemType(OrderItemType.RESERVATION)
                .sessionOptionId(101L)
                .qty(2)
                .unitPrice(14500)
                .lineAmount(29000)
                .build();
        mockItem.setId(10L);
        mockSavedOrder.setOrderItems(Arrays.asList(mockItem));
    }

    @Nested
    @DisplayName("주문 생성 성공 시나리오")
    class SuccessScenarios {

        @Test
        @DisplayName("예약형 주문 생성 성공")
        void createReservationOrder_Success() {
            // Given
            Long userId = 1001L;
            String idempotencyKey = "test-key-001";

            when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

            // When
            OrderCreatedDto result = orderService.createOrder(userId, reservationRequest, idempotencyKey);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOrderNo()).isEqualTo("O20251230-000001");
            assertThat(result.getOrderType()).isEqualTo("RESERVATION");
            assertThat(result.getStatus()).isEqualTo("REQUESTED");
            assertThat(result.getStoreId()).isEqualTo(1L);
            assertThat(result.getProductId()).isEqualTo(2L);
            assertThat(result.getTotalAmount()).isEqualTo(29000);
            assertThat(result.getItems()).hasSize(1);

            // Repository save 호출 검증
            verify(orderRepository, times(1)).save(any(Order.class));

            // 비동기 후처리 작업 호출 검증
            verify(orderAsyncService, times(1)).processOrderPostActions(1L);
            verify(orderAsyncService, times(1)).validateOrderAsync(
                eq(userId), eq(reservationRequest.getProductId()), eq(2)
            );
        }

        @Test
        @DisplayName("구매형 주문 생성 성공 (주소 정보 포함)")
        void createPurchaseOrder_Success() {
            // Given
            Long userId = 1001L;
            String idempotencyKey = "test-key-002";

            Order purchaseOrder = Order.builder()
                    .orderNo("O20251230-000002")
                    .customerId(userId)
                    .storeId(1L)
                    .productId(3L)
                    .orderType(OrderType.PURCHASE)
                    .status(OrderStatus.REQUESTED)
                    .totalAmount(29000)
                    .cancelableUntil(LocalDateTime.now().plusHours(1))
                    .build();
            purchaseOrder.setId(2L);
            OrderItem purchaseItem = OrderItem.builder()
                    .order(purchaseOrder)
                    .orderItemType(OrderItemType.MERCH)
                    .merchVariantId(201L)
                    .qty(1)
                    .unitPrice(14500)
                    .lineAmount(14500)
                    .build();
            purchaseItem.setId(20L);
            purchaseOrder.setOrderItems(Arrays.asList(purchaseItem));

            when(orderRepository.save(any(Order.class))).thenReturn(purchaseOrder);

            // When
            OrderCreatedDto result = orderService.createOrder(userId, purchaseRequest, idempotencyKey);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getOrderType()).isEqualTo("PURCHASE");
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getOrderItemType()).isEqualTo("MERCH");

            // Repository save 호출 검증
            verify(orderRepository, times(1)).save(any(Order.class));

            // 주소 정보는 현재 Order 엔티티에 없음
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("주문 생성 실패 시나리오")
    class FailureScenarios {

        @Test
        @DisplayName("주문 아이템이 없을 때 예외 발생")
        void createOrder_EmptyItems_ThrowsException() {
            // Given
            CreateOrderRequest emptyItemsRequest = CreateOrderRequest.builder()
                    .storeId(1L)
                    .productId(2L)
                    .orderType("RESERVATION")
                    .items(Collections.emptyList())
                    .build();

            // When & Then
            assertThatThrownBy(() ->
                orderService.createOrder(1001L, emptyItemsRequest, "test-key")
            )
            .isInstanceOf(OrderException.class);

            // Repository save가 호출되지 않았는지 검증
            verify(orderRepository, never()).save(any(Order.class));
            verify(orderAsyncService, never()).processOrderPostActions(any());
        }

        @Test
        @DisplayName("주문 아이템이 null일 때 예외 발생")
        void createOrder_NullItems_ThrowsException() {
            // Given
            CreateOrderRequest nullItemsRequest = CreateOrderRequest.builder()
                    .storeId(1L)
                    .productId(2L)
                    .orderType("RESERVATION")
                    .items(null)
                    .build();

            // When & Then
            assertThatThrownBy(() ->
                orderService.createOrder(1001L, nullItemsRequest, "test-key")
            )
            .isInstanceOf(OrderException.class);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("주문 타입이 유효하지 않으면 예외 발생")
        void createOrder_InvalidOrderType_ThrowsException() {
            // Given
            CreateOrderRequest invalidOrderTypeRequest = CreateOrderRequest.builder()
                    .storeId(1L)
                    .productId(2L)
                    .orderType("INVALID")
                    .items(reservationRequest.getItems())
                    .build();

            // When & Then
            assertThatThrownBy(() ->
                orderService.createOrder(1001L, invalidOrderTypeRequest, "test-key")
            )
            .isInstanceOf(IllegalArgumentException.class);

            verify(orderRepository, never()).save(any(Order.class));
            verify(orderAsyncService, never()).processOrderPostActions(any());
        }

        @Test
        @DisplayName("주문 아이템 타입이 유효하지 않으면 예외 발생")
        void createOrder_InvalidOrderItemType_ThrowsException() {
            // Given
            OrderItemRequest invalidItem = OrderItemRequest.builder()
                    .orderItemType("INVALID")
                    .qty(1)
                    .build();

            CreateOrderRequest invalidItemTypeRequest = CreateOrderRequest.builder()
                    .storeId(1L)
                    .productId(2L)
                    .orderType("RESERVATION")
                    .items(Arrays.asList(invalidItem))
                    .build();

            // When & Then
            assertThatThrownBy(() ->
                orderService.createOrder(1001L, invalidItemTypeRequest, "test-key")
            )
            .isInstanceOf(IllegalArgumentException.class);

            verify(orderRepository, never()).save(any(Order.class));
            verify(orderAsyncService, never()).processOrderPostActions(any());
        }

        @Test
        @DisplayName("비동기 검증 호출 중 예외 발생 시 저장하지 않음")
        void createOrder_AsyncValidationThrows_DoesNotSave() {
            // Given
            when(orderAsyncService.validateOrderAsync(anyLong(), anyLong(), anyInt()))
                    .thenThrow(new RuntimeException("async validation failed"));

            // When & Then
            assertThatThrownBy(() ->
                orderService.createOrder(1001L, reservationRequest, "test-key")
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("async validation failed");

            verify(orderRepository, never()).save(any(Order.class));
            verify(orderAsyncService, never()).processOrderPostActions(any());
        }
    }

    @Nested
    @DisplayName("비동기 처리 검증")
    class AsyncProcessingVerification {

        @Test
        @DisplayName("비동기 검증이 호출되는지 확인")
        void verifyAsyncValidationCalled() {
            // Given
            when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

            // When
            orderService.createOrder(1001L, reservationRequest, "test-key");

            // Then
            verify(orderAsyncService).validateOrderAsync(
                eq(1001L),                                    // userId
                eq(reservationRequest.getProductId()),        // productId
                eq(reservationRequest.getItems().get(0).getQty()) // qty
            );
        }

        @Test
        @DisplayName("비동기 후처리 작업이 호출되는지 확인")
        void verifyAsyncPostProcessingCalled() {
            // Given
            when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

            // When
            orderService.createOrder(1001L, reservationRequest, "test-key");

            // Then
            verify(orderAsyncService).processOrderPostActions(eq(1L));
        }
    }

    @Nested
    @DisplayName("DI 의존성 검증")
    class DependencyInjectionVerification {

        @Test
        @DisplayName("OrderRepository가 정상 주입되었는지 확인")
        void verifyOrderRepositoryInjection() {
            // Given
            when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

            // When
            orderService.createOrder(1001L, reservationRequest, "test-key");

            // Then
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("OrderAsyncService가 정상 주입되었는지 확인")
        void verifyOrderAsyncServiceInjection() {
            // Given
            when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

            // When
            orderService.createOrder(1001L, reservationRequest, "test-key");

            // Then
            verify(orderAsyncService).validateOrderAsync(any(), any(), any());
            verify(orderAsyncService).processOrderPostActions(any());
        }
    }
}
