package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;

/**
 * OrderDomainService 단위 테스트
 *
 * Clean Architecture의 Domain Layer 테스트
 * - 순수 비즈니스 로직 검증
 * - 외부 의존성 없이 테스트
 * - 도메인 규칙과 불변식 검증
 */
@DisplayName("주문 도메인 서비스 테스트")
class OrderDomainServiceTest {

    private OrderDomainService orderDomainService;

    @BeforeEach
    void setUp() {
        orderDomainService = new OrderDomainService();
    }

    @Test
    @DisplayName("중복 주문 검증 - 멱등성 키가 없으면 중복이 아님")
    void isDuplicateOrder_NoIdempotencyKey_ReturnsFalse() {
        // given
        Optional<Order> existingOrder = Optional.empty();
        String idempotencyKey = null;

        // when
        boolean isDuplicate = orderDomainService.isDuplicateOrder(existingOrder, idempotencyKey);

        // then
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("중복 주문 검증 - 기존 주문이 있으면 중복임")
    void isDuplicateOrder_ExistingOrder_ReturnsTrue() {
        // given
        Order existingOrder = createSampleOrder();
        String idempotencyKey = "test-key";

        // when
        boolean isDuplicate = orderDomainService.isDuplicateOrder(Optional.of(existingOrder), idempotencyKey);

        // then
        assertThat(isDuplicate).isTrue();
    }

    @Test
    @DisplayName("주문 생성 검증 - 정상적인 주문은 통과")
    void validateOrderCreation_ValidOrder_DoesNotThrow() {
        // given
        Long customerId = 1001L;
        Long storeId = 1L;
        Long productId = 1L;
        List<OrderItem> orderItems = createSampleOrderItems();

        // when & then
        // when & then - 예외가 발생하지 않으면 성공
        try {
            orderDomainService.validateOrderCreation(customerId, storeId, productId, orderItems);
        } catch (Exception e) {
            throw new AssertionError("예외가 발생하지 않아야 함", e);
        }
    }

    @Test
    @DisplayName("주문 생성 검증 - 고객 ID가 null이면 예외 발생")
    void validateOrderCreation_NullCustomerId_ThrowsException() {
        // given
        Long customerId = null;
        Long storeId = 1L;
        Long productId = 1L;
        List<OrderItem> orderItems = createSampleOrderItems();

        // when & then
        assertThatThrownBy(() -> orderDomainService.validateOrderCreation(customerId, storeId, productId, orderItems))
                .isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("주문 생성 검증 - 주문 항목이 비어있으면 예외 발생")
    void validateOrderCreation_EmptyItems_ThrowsException() {
        // given
        Long customerId = 1001L;
        Long storeId = 1L;
        Long productId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();

        // when & then
        assertThatThrownBy(() -> orderDomainService.validateOrderCreation(customerId, storeId, productId, orderItems))
                .isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("취소 가능 시간 계산 - 예약형은 1일 후")
    void calculateCancelableUntil_Reservation_Returns1DayLater() {
        // given
        OrderType orderType = OrderType.RESERVATION;
        LocalDateTime before = LocalDateTime.now().plusDays(1).minusMinutes(1);
        LocalDateTime after = LocalDateTime.now().plusDays(1).plusMinutes(1);

        // when
        LocalDateTime cancelableUntil = orderDomainService.calculateCancelableUntil(orderType);

        // then
        assertThat(cancelableUntil).isBetween(before, after);
    }

    @Test
    @DisplayName("취소 가능 시간 계산 - 구매형은 1시간 후")
    void calculateCancelableUntil_Purchase_Returns1HourLater() {
        // given
        OrderType orderType = OrderType.PURCHASE;
        LocalDateTime before = LocalDateTime.now().plusHours(1).minusMinutes(1);
        LocalDateTime after = LocalDateTime.now().plusHours(1).plusMinutes(1);

        // when
        LocalDateTime cancelableUntil = orderDomainService.calculateCancelableUntil(orderType);

        // then
        assertThat(cancelableUntil).isBetween(before, after);
    }

    @Test
    @DisplayName("총액 계산 - 수량과 단가의 곱셈 합계")
    void calculateTotalAmount_MultipleItems_ReturnsCorrectSum() {
        // given
        List<OrderItem> orderItems = List.of(
                createOrderItem(10000, 2), // 20,000
                createOrderItem(15000, 1)  // 15,000
        );

        // when
        int totalAmount = orderDomainService.calculateTotalAmount(orderItems);

        // then
        assertThat(totalAmount).isEqualTo(35000);
    }

    @Test
    @DisplayName("주문 엔티티 생성 - 모든 필드가 올바르게 설정됨")
    void createOrder_ValidInput_CreatesOrderWithCorrectFields() {
        // given
        Long customerId = 1001L;
        Long storeId = 1L;
        Long productId = 1L;
        OrderType orderType = OrderType.RESERVATION;
        List<OrderItem> orderItems = createSampleOrderItems();
        String idempotencyKey = "test-key-001";

        // when
        Order order = orderDomainService.createOrder(customerId, storeId, productId, orderType, orderItems, idempotencyKey);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStoreId()).isEqualTo(storeId);
        assertThat(order.getProductId()).isEqualTo(productId);
        assertThat(order.getOrderType()).isEqualTo(orderType);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(order.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(order.getOrderNo()).isNotEmpty();
        assertThat(order.getTotalAmount()).isPositive();
        assertThat(order.getCancelableUntil()).isAfter(LocalDateTime.now());
        assertThat(order.getOrderItems()).hasSize(orderItems.size());
    }

    @Test
    @DisplayName("주문 상태 변경 가능 검증 - REQUESTED에서 CONFIRMED로 변경 가능")
    void canChangeStatus_RequestedToConfirmed_ReturnsTrue() {
        // given
        OrderStatus currentStatus = OrderStatus.REQUESTED;
        OrderStatus newStatus = OrderStatus.CONFIRMED;

        // when
        boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

        // then
        assertThat(canChange).isTrue();
    }

    @Test
    @DisplayName("주문 상태 변경 가능 검증 - COMPLETED에서 변경 불가")
    void canChangeStatus_CompletedToAny_ReturnsFalse() {
        // given
        OrderStatus currentStatus = OrderStatus.COMPLETED;
        OrderStatus newStatus = OrderStatus.CANCELLED;

        // when
        boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

        // then
        assertThat(canChange).isFalse();
    }

    // Helper methods
    private Order createSampleOrder() {
        return Order.builder()
                .id(1L)
                .orderNo("O20231230-000001")
                .customerId(1001L)
                .storeId(1L)
                .productId(1L)
                .orderType(OrderType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .totalAmount(29000)
                .idempotencyKey("test-key")
                .orderItems(new ArrayList<>())
                .build();
    }

    private List<OrderItem> createSampleOrderItems() {
        return List.of(
                createOrderItem(14500, 2)
        );
    }

    private OrderItem createOrderItem(int unitPrice, int qty) {
        return OrderItem.builder()
                .orderItemType(OrderItemType.RESERVATION)
                .qty(qty)
                .unitPrice(unitPrice)
                .lineAmount(unitPrice * qty)
                .sessionOptionId(1L)
                .build();
    }
}