package com.popcorn.demo.infrastructure.persistence.repository;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 도메인 OrderRepository 인터페이스의 JPA 구현체
 * - Clean Architecture: 어댑터 패턴 적용
 * - 도메인 레이어의 인터페이스를 인프라스트럭처 레이어에서 구현
 * - JpaOrderRepository를 래핑하여 도메인 요구사항 충족
 */
@Repository("orderRepositoryImpl")
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    /**
     * 생성자 기반 의존성 주입
     * @param jpaOrderRepository JPA Repository
     */
    @Autowired
    public OrderRepositoryImpl(JpaOrderRepository jpaOrderRepository) {
        this.jpaOrderRepository = jpaOrderRepository;
    }

    // ========================= 기본 CRUD 메서드 =========================

    @Override
    public Order save(Order order) {
        return jpaOrderRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return jpaOrderRepository.findById(orderId);
    }

    @Override
    public Optional<OrderSummaryView> findSummaryById(Long orderId) {
        return jpaOrderRepository.findSummaryById(orderId);
    }

    @Override
    public Optional<Order> findByOrderNo(String orderNo) {
        return jpaOrderRepository.findByOrderNo(orderNo);
    }

    @Override
    public void deleteById(Long orderId) {
        jpaOrderRepository.deleteById(orderId);
    }

    @Override
    public boolean existsById(Long orderId) {
        return jpaOrderRepository.existsById(orderId);
    }

    // ========================= 비즈니스 조회 메서드 =========================

    @Override
    public List<Order> findByCustomerId(Long customerId) {
        return jpaOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public List<Order> findByCustomerId(Long customerId, int offset, int limit) {
        // offset/limit을 PageRequest로 변환 (offset은 page 번호가 아니라 실제 offset)
        int page = offset / limit;
        PageRequest pageRequest = PageRequest.of(page, limit);
        return jpaOrderRepository.findByCustomerIdWithPaging(customerId, pageRequest);
    }

    @Override
    public List<Order> findByStoreId(Long storeId) {
        return jpaOrderRepository.findByStoreId(storeId);
    }

    @Override
    public List<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status) {
        return jpaOrderRepository.findByStoreIdAndStatus(storeId, status);
    }

    @Override
    public List<Order> findByProductId(Long productId) {
        return jpaOrderRepository.findByProductId(productId);
    }

    // ========================= 상태별 조회 메서드 =========================

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaOrderRepository.findByStatus(status);
    }

    @Override
    public List<Order> findCancelableOrders(LocalDateTime currentTime) {
        return jpaOrderRepository.findCancelableOrders(currentTime);
    }

    @Override
    public List<Order> findExpiredCancelableOrders(LocalDateTime currentTime) {
        return jpaOrderRepository.findExpiredCancelableOrders(currentTime);
    }

    // ========================= 통계 및 집계 메서드 =========================

    @Override
    public long countByCustomerId(Long customerId) {
        return jpaOrderRepository.countByCustomerId(customerId);
    }

    @Override
    public long countByStoreId(Long storeId) {
        return jpaOrderRepository.countByStoreId(storeId);
    }

    @Override
    public long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return jpaOrderRepository.countByCreatedAtBetween(startDate, endDate);
    }

    @Override
    public long sumTotalAmountByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        return jpaOrderRepository.sumTotalAmountByCustomerIdAndCreatedAtBetween(customerId, startDate, endDate);
    }

    // ========================= 중복 방지 메서드 =========================
    // 향후 Order 엔티티에 idempotencyKey 필드 추가 시 구현

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return jpaOrderRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaOrderRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
