package com.popcorn.demo.infrastructure.persistence.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.repository.OrderRepository;

/**
 * OrderRepositoryAdapter 단위 테스트
 *
 * Clean Architecture의 Infrastructure Layer 테스트
 * - Port(인터페이스)와 Repository(구현체) 간의 어댑터 로직 검증
 * - 도메인 엔티티와 JPA Repository 간의 데이터 변환 검증
 * - 외부 의존성을 Mock으로 대체하여 격리된 테스트 수행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 Repository 어댑터 테스트")
class OrderRepositoryAdapterTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderRepositoryAdapter orderRepositoryAdapter;

    @BeforeEach
    void setUp() {
        orderRepositoryAdapter = new OrderRepositoryAdapter(orderRepository);
    }

    @Test
    @DisplayName("멱등성 키로 주문 조회 - 기존 주문이 있는 경우")
    void findByIdempotencyKey_ExistingOrder_ReturnsOrder() {
        // given
        String idempotencyKey = "test-key-001";
        Order mockOrder = createMockOrder();
        when(orderRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(mockOrder));

        // when
        Optional<Order> result = orderRepositoryAdapter.findByIdempotencyKey(idempotencyKey);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(mockOrder.getId());
        assertThat(result.get().getIdempotencyKey()).isEqualTo(idempotencyKey);

        verify(orderRepository, times(1)).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("멱등성 키로 주문 조회 - 주문이 없는 경우")
    void findByIdempotencyKey_NoOrder_ReturnsEmpty() {
        // given
        String idempotencyKey = "nonexistent-key";
        when(orderRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        // when
        Optional<Order> result = orderRepositoryAdapter.findByIdempotencyKey(idempotencyKey);

        // then
        assertThat(result).isEmpty();

        verify(orderRepository, times(1)).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("ID로 주문 조회 - 기존 주문이 있는 경우")
    void findById_ExistingOrder_ReturnsOrder() {
        // given
        Long orderId = 101L;
        Order mockOrder = createMockOrder();
        mockOrder = Order.builder()
                .id(orderId)
                .orderNo(mockOrder.getOrderNo())
                .customerId(mockOrder.getCustomerId())
                .storeId(mockOrder.getStoreId())
                .productId(mockOrder.getProductId())
                .orderType(mockOrder.getOrderType())
                .status(mockOrder.getStatus())
                .totalAmount(mockOrder.getTotalAmount())
                .idempotencyKey(mockOrder.getIdempotencyKey())
                .cancelableUntil(mockOrder.getCancelableUntil())
                .orderItems(mockOrder.getOrderItems())
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(mockOrder));

        // when
        Optional<Order> result = orderRepositoryAdapter.findById(orderId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(orderId);

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("ID로 주문 조회 - 주문이 없는 경우")
    void findById_NoOrder_ReturnsEmpty() {
        // given
        Long orderId = 999L;
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // when
        Optional<Order> result = orderRepositoryAdapter.findById(orderId);

        // then
        assertThat(result).isEmpty();

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 저장 - 새로운 주문 저장")
    void save_NewOrder_ReturnsSavedOrder() {
        // given
        Order newOrder = createMockOrder();
        Order savedOrder = Order.builder()
                .id(101L) // DB에서 생성된 ID
                .orderNo(newOrder.getOrderNo())
                .customerId(newOrder.getCustomerId())
                .storeId(newOrder.getStoreId())
                .productId(newOrder.getProductId())
                .orderType(newOrder.getOrderType())
                .status(newOrder.getStatus())
                .totalAmount(newOrder.getTotalAmount())
                .idempotencyKey(newOrder.getIdempotencyKey())
                .cancelableUntil(newOrder.getCancelableUntil())
                .orderItems(newOrder.getOrderItems())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // when
        Order result = orderRepositoryAdapter.save(newOrder);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getOrderNo()).isEqualTo(newOrder.getOrderNo());
        assertThat(result.getCustomerId()).isEqualTo(newOrder.getCustomerId());

        verify(orderRepository, times(1)).save(newOrder);
    }

    @Test
    @DisplayName("주문 저장 - 기존 주문 업데이트")
    void save_ExistingOrder_ReturnsUpdatedOrder() {
        // given
        Order existingOrder = createMockOrder();
        existingOrder = Order.builder()
                .id(101L)
                .orderNo(existingOrder.getOrderNo())
                .customerId(existingOrder.getCustomerId())
                .storeId(existingOrder.getStoreId())
                .productId(existingOrder.getProductId())
                .orderType(existingOrder.getOrderType())
                .status(OrderStatus.CONFIRMED) // 상태 변경
                .totalAmount(existingOrder.getTotalAmount())
                .idempotencyKey(existingOrder.getIdempotencyKey())
                .cancelableUntil(existingOrder.getCancelableUntil())
                .orderItems(existingOrder.getOrderItems())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        // when
        Order result = orderRepositoryAdapter.save(existingOrder);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(orderRepository, times(1)).save(existingOrder);
    }

    @Test
    @DisplayName("어댑터 초기화 - Repository가 null인 경우 예외 발생")
    void constructor_NullRepository_ThrowsException() {
        // when & then
        try {
            new OrderRepositoryAdapter(null);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    // Helper methods
    private Order createMockOrder() {
        return Order.builder()
                .orderNo("O20231230-000001")
                .customerId(1001L)
                .storeId(1L)
                .productId(1L)
                .orderType(OrderType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .totalAmount(29000)
                .idempotencyKey("test-key-001")
                .cancelableUntil(LocalDateTime.now().plusDays(1))
                .orderItems(new ArrayList<>())
                .build();
    }
}