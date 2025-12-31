package com.popcorn.demo.infrastructure.persistence.repository;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryImplTest {

    @Mock
    private JpaOrderRepository jpaOrderRepository;

    private OrderRepositoryImpl orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepositoryImpl(jpaOrderRepository);
    }

    @Test
    @DisplayName("Save - JpaOrderRepository delegate")
    void save_DelegatesToJpaOrderRepository() {
        // given
        Order order = buildOrder("O-1001", 1001L, 10L, 55L, OrderStatus.REQUESTED, 29000, null);
        Order savedOrder = buildOrder("O-1001", 1001L, 10L, 55L, OrderStatus.REQUESTED, 29000, null);
        savedOrder = Order.builder()
                .id(1L)
                .orderNo(savedOrder.getOrderNo())
                .customerId(savedOrder.getCustomerId())
                .storeId(savedOrder.getStoreId())
                .productId(savedOrder.getProductId())
                .orderType(savedOrder.getOrderType())
                .status(savedOrder.getStatus())
                .totalAmount(savedOrder.getTotalAmount())
                .build();

        when(jpaOrderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // when
        Order result = orderRepository.save(order);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        verify(jpaOrderRepository).save(order);
    }

    @Test
    @DisplayName("FindById - JpaOrderRepository delegate")
    void findById_DelegatesToJpaOrderRepository() {
        // given
        Order order = buildOrder("O-1001", 1001L, 10L, 55L, OrderStatus.REQUESTED, 29000, null);
        when(jpaOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        // when
        Optional<Order> result = orderRepository.findById(1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNo()).isEqualTo("O-1001");
        verify(jpaOrderRepository).findById(1L);
    }

    @Test
    @DisplayName("FindByOrderNo - JpaOrderRepository delegate")
    void findByOrderNo_DelegatesToJpaOrderRepository() {
        // given
        Order order = buildOrder("O-2001", 1002L, 11L, 56L, OrderStatus.REQUESTED, 15000, null);
        when(jpaOrderRepository.findByOrderNo("O-2001")).thenReturn(Optional.of(order));

        // when
        Optional<Order> result = orderRepository.findByOrderNo("O-2001");

        // then
        assertThat(result).isPresent();
        verify(jpaOrderRepository).findByOrderNo("O-2001");
    }

    @Test
    @DisplayName("ExistsById - JpaOrderRepository delegate")
    void existsById_DelegatesToJpaOrderRepository() {
        // given
        when(jpaOrderRepository.existsById(99999L)).thenReturn(false);

        // when
        boolean result = orderRepository.existsById(99999L);

        // then
        assertThat(result).isFalse();
        verify(jpaOrderRepository).existsById(99999L);
    }

    @Test
    @DisplayName("DeleteById - JpaOrderRepository delegate")
    void deleteById_DelegatesToJpaOrderRepository() {
        // when
        orderRepository.deleteById(1L);

        // then
        verify(jpaOrderRepository).deleteById(1L);
    }

    private Order buildOrder(String orderNo, Long customerId, Long storeId, Long productId,
                             OrderStatus status, int totalAmount, LocalDateTime cancelableUntil) {
        return Order.builder()
                .orderNo(orderNo)
                .customerId(customerId)
                .storeId(storeId)
                .productId(productId)
                .orderType(OrderType.RESERVATION)
                .status(status)
                .totalAmount(totalAmount)
                .cancelableUntil(cancelableUntil)
                .build();
    }
}
