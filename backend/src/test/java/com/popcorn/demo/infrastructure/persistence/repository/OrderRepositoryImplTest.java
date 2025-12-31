package com.popcorn.demo.infrastructure.persistence.repository;

import com.popcorn.demo.DemoApplication;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({OrderRepositoryImpl.class, OrderRepositoryImplTest.JpaTestConfig.class})
class OrderRepositoryImplTest {

    @TestConfiguration
    @AutoConfigurationPackage(basePackageClasses = DemoApplication.class)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.popcorn.demo.domain.order.entity")
    @EnableJpaRepositories(basePackages = "com.popcorn.demo.infrastructure.persistence.repository")
    @EnableJpaAuditing
    static class JpaTestConfig {
    }

    @Autowired
    private OrderRepositoryImpl orderRepository;

    @Test
    @DisplayName("Save and find by id")
    void saveAndFindById_Success() {
        Order order = buildOrder("O-1001", 1001L, 10L, 55L, OrderStatus.REQUESTED, 29000, null);
        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(orderRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Find by orderNo and existsById")
    void findByOrderNo_AndExistsById() {
        Order order = buildOrder("O-2001", 1002L, 11L, 56L, OrderStatus.REQUESTED, 15000, null);
        orderRepository.save(order);

        assertThat(orderRepository.findByOrderNo("O-2001")).isPresent();
        assertThat(orderRepository.existsById(99999L)).isFalse();
    }

    @Test
    @DisplayName("Find by store and status")
    void findByStoreIdAndStatus() {
        orderRepository.save(buildOrder("O-3001", 1003L, 12L, 57L, OrderStatus.REQUESTED, 10000, null));
        orderRepository.save(buildOrder("O-3002", 1004L, 12L, 57L, OrderStatus.CANCELLED, 20000, null));

        List<Order> requestedOrders = orderRepository.findByStoreIdAndStatus(12L, OrderStatus.REQUESTED);

        assertThat(requestedOrders).hasSize(1);
        assertThat(requestedOrders.get(0).getOrderNo()).isEqualTo("O-3001");
    }

    @Test
    @DisplayName("Find by status and cancelable queries")
    void findByStatusAndCancelableQueries() {
        LocalDateTime now = LocalDateTime.now();
        orderRepository.save(buildOrder("O-4001", 1005L, 13L, 58L, OrderStatus.REQUESTED, 12000, now.plusHours(1)));
        orderRepository.save(buildOrder("O-4002", 1006L, 13L, 58L, OrderStatus.REQUESTED, 13000, now.minusHours(1)));

        assertThat(orderRepository.findByStatus(OrderStatus.REQUESTED)).hasSize(2);
        assertThat(orderRepository.findCancelableOrders(now)).hasSize(1);
        assertThat(orderRepository.findExpiredCancelableOrders(now)).hasSize(1);
    }

    @Test
    @DisplayName("Count and sum aggregations")
    void countAndSumAggregations() {
        LocalDateTime now = LocalDateTime.now();
        orderRepository.save(buildOrder("O-5001", 1007L, 14L, 59L, OrderStatus.REQUESTED, 10000, now.plusHours(1)));
        orderRepository.save(buildOrder("O-5002", 1007L, 14L, 60L, OrderStatus.REQUESTED, 20000, now.plusHours(1)));

        assertThat(orderRepository.countByCustomerId(1007L)).isEqualTo(2);
        assertThat(orderRepository.countByStoreId(14L)).isEqualTo(2);
        assertThat(orderRepository.countByCreatedAtBetween(now.minusMinutes(1), now.plusMinutes(1))).isEqualTo(2);
        assertThat(orderRepository.sumTotalAmountByCustomerIdAndCreatedAtBetween(
                1007L,
                now.minusMinutes(1),
                now.plusMinutes(1)
        )).isEqualTo(30000);
    }

    @Test
    @DisplayName("Paging and delete")
    void pagingAndDelete() {
        for (int i = 0; i < 5; i++) {
            orderRepository.save(buildOrder("O-600" + i, 2001L, 15L, 61L + i, OrderStatus.REQUESTED, 10000, null));
        }

        assertThat(orderRepository.findByCustomerId(2001L, 0, 2)).hasSize(2);
        assertThat(orderRepository.findByCustomerId(2001L, 2, 2)).hasSize(2);

        Order order = orderRepository.findByOrderNo("O-6000").orElseThrow();
        orderRepository.deleteById(order.getId());

        assertThat(orderRepository.findByOrderNo("O-6000")).isEmpty();
    }

    @Test
    @DisplayName("Missing order returns empty")
    void findMissingOrder_ReturnsEmpty() {
        assertThat(orderRepository.findById(9999L)).isEmpty();
        assertThat(orderRepository.findByOrderNo("O-9999")).isEmpty();
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
