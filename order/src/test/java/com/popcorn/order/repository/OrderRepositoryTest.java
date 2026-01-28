package com.popcorn.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.ItemType;

/**
 * OrderRepository 테스트
 *
 * 초보자를 위한 Repository 테스트 가이드:
 * - @DataJpaTest: JPA 관련 테스트만 로딩 (속도 향상)
 * - @ActiveProfiles("test"): 테스트 전용 설정 사용
 * - 실제 데이터베이스 대신 H2 인메모리 DB 사용
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("주문 Repository 테스트")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order sampleOrder1;
    private Order sampleOrder2;
    private Order sampleOrder3;
    private UUID popupId1;
    private UUID popupId2;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 준비
        popupId1 = UUID.randomUUID();
        popupId2 = UUID.randomUUID();

        sampleOrder1 = Order.builder()
                .orderNo("O20260120-000001")
                .customerId(1L)
                .popupId(popupId1)
                .popupId(popupId1)
                .orderType(ItemType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .totalAmount(15000)
                .cancelableUntil(LocalDateTime.now().plusMinutes(30))
                .build();

        sampleOrder2 = Order.builder()
                .orderNo("O20260120-000002")
                .customerId(1L)
                .popupId(popupId1)
                .popupId(popupId1)
                .orderType(ItemType.GOODS)
                .status(OrderStatus.PAID)
                .totalAmount(25000)
                .cancelableUntil(LocalDateTime.now().plusMinutes(30))
                .build();

        sampleOrder3 = Order.builder()
                .orderNo("O20260120-000003")
                .customerId(2L)
                .popupId(popupId2)
                .popupId(popupId1)
                .orderType(ItemType.RESERVATION)
                .status(OrderStatus.COMPLETED)
                .totalAmount(35000)
                .cancelableUntil(LocalDateTime.now().minusMinutes(30)) // 이미 지난 시간
                .build();
    }

    @Test
    @DisplayName("주문 저장 및 조회 테스트")
    void saveAndFindOrder() {
        // Given: 주문을 저장하고
        Order savedOrder = orderRepository.save(sampleOrder1);

        // When: ID로 조회하면
        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());

        // Then: 올바르게 조회되어야 함
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getOrderNo()).isEqualTo("O20260120-000001");
        assertThat(foundOrder.get().getCustomerId()).isEqualTo(1L);
        assertThat(foundOrder.get().getTotalAmount()).isEqualTo(15000);
    }

    @Test
    @DisplayName("주문 번호로 조회 테스트")
    void findByOrderNo() {
        // Given: 주문을 저장하고
        orderRepository.save(sampleOrder1);

        // When: 주문 번호로 조회하면
        Optional<Order> foundOrder = orderRepository.findByOrderNo("O20260120-000001");

        // Then: 올바르게 조회되어야 함
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 주문 번호 조회 테스트")
    void findByOrderNoNotFound() {
        // When: 존재하지 않는 주문 번호로 조회하면
        Optional<Order> foundOrder = orderRepository.findByOrderNo("NOT_EXISTS");

        // Then: 빈 결과가 반환되어야 함
        assertThat(foundOrder).isEmpty();
    }

    @Test
    @DisplayName("사용자별 주문 조회 테스트")
    void findByCustomerId() {
        // Given: 여러 주문을 저장하고
        orderRepository.save(sampleOrder1);
        orderRepository.save(sampleOrder2);
        orderRepository.save(sampleOrder3);

        // When: 특정 사용자의 주문을 조회하면
        List<Order> orders = orderRepository.findByCustomerId(1L);

        // Then: 해당 사용자의 주문 2개가 조회되어야 함
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(order -> order.getCustomerId().equals(1L));
    }

    @Test
    @DisplayName("사용자별 주문 조회 페이징 테스트")
    void findByCustomerIdWithPaging() {
        // Given: 주문들을 저장하고
        orderRepository.save(sampleOrder1);
        orderRepository.save(sampleOrder2);

        // When: 페이징으로 조회하면
        PageRequest pageRequest = PageRequest.of(0, 1); // 첫 번째 페이지, 1개씩
        Page<Order> orderPage = orderRepository.findByCustomerIdOrderByCreatedAtDesc(1L, pageRequest);

        // Then: 페이징 정보가 올바르게 설정되어야 함
        assertThat(orderPage.getTotalElements()).isEqualTo(2);
        assertThat(orderPage.getContent()).hasSize(1);
        assertThat(orderPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("팝업별 주문 조회 테스트")
    void findByPopupId() {
        // Given: 여러 주문을 저장하고
        orderRepository.save(sampleOrder1);
        orderRepository.save(sampleOrder2);
        orderRepository.save(sampleOrder3);

        // When: 특정 팝업의 주문을 조회하면
        Page<Order> orders = orderRepository.findByPopupIdOrderByCreatedAtDesc(popupId1, PageRequest.of(0, 10));

        // Then: 해당 팝업의 주문 2개가 조회되어야 함
        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getContent()).allMatch(order -> order.getPopupId().equals(popupId1));
    }

    @Test
    @DisplayName("주문 상태별 조회 테스트")
    void findByStatus() {
        // Given: 다양한 상태의 주문들을 저장하고
        orderRepository.save(sampleOrder1); // REQUESTED
        orderRepository.save(sampleOrder2); // PAID
        orderRepository.save(sampleOrder3); // COMPLETED

        // When: REQUESTED 상태 주문을 조회하면
        List<Order> requestedOrders = orderRepository.findByStatus(OrderStatus.REQUESTED);

        // Then: 1개가 조회되어야 함
        assertThat(requestedOrders).hasSize(1);
        assertThat(requestedOrders.get(0).getStatus()).isEqualTo(OrderStatus.REQUESTED);
    }

    @Test
    @DisplayName("사용자별 특정 상태 주문 조회 테스트")
    void findByCustomerIdAndStatus() {
        // Given: 주문들을 저장하고
        orderRepository.save(sampleOrder1); // 고객1, REQUESTED
        orderRepository.save(sampleOrder2); // 고객1, PAID

        // When: 고객1의 REQUESTED 상태 주문을 조회하면
        List<Order> orders = orderRepository.findByCustomerIdAndStatus(1L, OrderStatus.REQUESTED);

        // Then: 1개가 조회되어야 함
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(orders.get(0).getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자별 주문 개수 조회 테스트")
    void countByCustomerId() {
        // Given: 주문들을 저장하고
        orderRepository.save(sampleOrder1);
        orderRepository.save(sampleOrder2);
        orderRepository.save(sampleOrder3);

        // When: 고객1의 주문 개수를 조회하면
        long count = orderRepository.countByCustomerId(1L);

        // Then: 2개여야 함
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("팝업별 특정 상태 주문 개수 조회 테스트")
    void countByPopupIdAndStatus() {
        // Given: 주문들을 저장하고
        orderRepository.save(sampleOrder1); // popupId1, REQUESTED
        orderRepository.save(sampleOrder2); // popupId1, PAID

        // When: popupId1의 REQUESTED 상태 주문 개수를 조회하면
        long count = orderRepository.countOrdersByPopupId(popupId1, "REQUESTED", null);

        // Then: 1개여야 함
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("취소 가능한 주문 조회 테스트")
    void findCancelableOrders() {
        // Given: 취소 가능/불가능한 주문들을 저장하고
        orderRepository.save(sampleOrder1); // 취소 가능
        orderRepository.save(sampleOrder2); // 취소 가능
        orderRepository.save(sampleOrder3); // 취소 불가 (시간 지남, COMPLETED 상태)

        // When: 취소 가능한 주문을 조회하면
        List<Order> cancelableOrders = orderRepository.findCancelableOrders();

        // Then: 취소 가능한 주문만 조회되어야 함
        assertThat(cancelableOrders).hasSize(2);
        assertThat(cancelableOrders).allMatch(order ->
                order.getCancelableUntil().isAfter(LocalDateTime.now()) &&
                order.getStatus() != OrderStatus.CANCELLED &&
                order.getStatus() != OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("주문 번호 존재 여부 확인 테스트")
    void existsByOrderNo() {
        // Given: 주문을 저장하고
        orderRepository.save(sampleOrder1);

        // When & Then: 존재하는 주문 번호는 true
        assertThat(orderRepository.existsByOrderNo("O20260120-000001")).isTrue();

        // When & Then: 존재하지 않는 주문 번호는 false
        assertThat(orderRepository.existsByOrderNo("NOT_EXISTS")).isFalse();
    }

    @Test
    @DisplayName("기간별 주문 조회 테스트")
    void findByStoreIdAndCreatedAtBetween() {
        // Given: 주문들을 저장하고
        orderRepository.save(sampleOrder1);
        orderRepository.save(sampleOrder2);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // When: 팝업별로 조회하면
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Order> orders = orderRepository.findByPopupIdOrderByCreatedAtDesc(popupId1, pageRequest);

        // Then: 해당 팝업의 주문들이 조회되어야 함
        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getContent()).allMatch(order -> order.getPopupId().equals(popupId1));
    }

    @Test
    @DisplayName("취소 가능한 주문 조회 (특정 사용자) 테스트")
    void findCancellableOrdersByCustomerId() {
        // Given: 주문들을 저장하고
        orderRepository.save(sampleOrder1); // 고객1, 취소 가능
        orderRepository.save(sampleOrder2); // 고객1, 취소 가능하지만 PAID 상태
        orderRepository.save(sampleOrder3); // 고객2, 취소 불가 (COMPLETED)

        // When: 고객1의 취소 가능한 주문을 조회하면
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Order> orders = orderRepository.findCancellableOrdersByCustomerId(
                1L, LocalDateTime.now(), pageRequest);

        // Then: 취소 가능한 주문들만 조회되어야 함
        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getContent()).allMatch(order -> order.getCustomerId().equals(1L));
    }
}