package com.popcorn.demo.domain.order.repository;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 레포지토리 인터페이스
 * - 클린 아키텍처: 도메인 계층에서 인터페이스 정의
 * - 인프라스트럭처 계층에서 구현체 제공
 * - 도메인 엔티티 Order를 다루는 데이터 액세스 추상화
 */
public interface OrderRepository {
    
    // ========================= 기본 CRUD 메서드 =========================
    
    /**
     * 주문 저장 (생성/수정)
     * @param order 저장할 주문 엔티티
     * @return 저장된 주문 엔티티 (ID 할당됨)
     */
    Order save(Order order);
    
    /**
     * 주문 ID로 조회
     * @param orderId 주문 ID
     * @return 주문 엔티티 (Optional)
     */
    Optional<Order> findById(Long orderId);

    /**
     * 주문 요약 조회 (필요 컬럼만)
     * @param orderId 주문 ID
     * @return 주문 요약 정보
     */
    Optional<OrderSummaryView> findSummaryById(Long orderId);
    
    /**
     * 주문 번호로 조회
     * @param orderNo 주문 번호
     * @return 주문 엔티티 (Optional)
     */
    Optional<Order> findByOrderNo(String orderNo);
    
    /**
     * 주문 삭제
     * @param orderId 삭제할 주문 ID
     */
    void deleteById(Long orderId);
    
    /**
     * 주문 존재 여부 확인
     * @param orderId 주문 ID
     * @return 존재하면 true
     */
    boolean existsById(Long orderId);
    
    // ========================= 비즈니스 조회 메서드 =========================
    
    /**
     * 고객의 주문 목록 조회
     * @param customerId 고객 ID
     * @return 고객의 주문 목록
     */
    List<Order> findByCustomerId(Long customerId);
    
    /**
     * 고객의 주문 목록 조회 (페이징)
     * @param customerId 고객 ID
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 고객의 주문 목록
     */
    List<Order> findByCustomerId(Long customerId, int offset, int limit);
    
    /**
     * 스토어의 주문 목록 조회
     * @param storeId 스토어 ID
     * @return 스토어의 주문 목록
     */
    List<Order> findByStoreId(Long storeId);
    
    /**
     * 스토어의 주문 목록 조회 (특정 상태)
     * @param storeId 스토어 ID
     * @param status 주문 상태
     * @return 해당 상태의 스토어 주문 목록
     */
    List<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status);
    
    /**
     * 상품의 주문 목록 조회
     * @param productId 상품 ID
     * @return 상품의 주문 목록
     */
    List<Order> findByProductId(Long productId);
    
    // ========================= 상태별 조회 메서드 =========================
    
    /**
     * 특정 상태의 주문 목록 조회
     * @param status 주문 상태
     * @return 해당 상태의 주문 목록
     */
    List<Order> findByStatus(OrderStatus status);
    
    /**
     * 취소 가능한 주문 목록 조회
     * @param currentTime 현재 시간
     * @return 취소 가능한 주문 목록 (cancelable_until > currentTime)
     */
    List<Order> findCancelableOrders(LocalDateTime currentTime);
    
    /**
     * 취소 기간이 만료된 주문 목록 조회
     * @param currentTime 현재 시간
     * @return 취소 기간 만료된 주문 목록 (cancelable_until <= currentTime)
     */
    List<Order> findExpiredCancelableOrders(LocalDateTime currentTime);
    
    // ========================= 통계 및 집계 메서드 =========================
    
    /**
     * 고객의 총 주문 수 조회
     * @param customerId 고객 ID
     * @return 총 주문 수
     */
    long countByCustomerId(Long customerId);
    
    /**
     * 스토어의 총 주문 수 조회
     * @param storeId 스토어 ID
     * @return 총 주문 수
     */
    long countByStoreId(Long storeId);
    
    /**
     * 특정 기간 내 주문 수 조회
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 해당 기간 주문 수
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 고객의 특정 기간 총 주문 금액 조회
     * @param customerId 고객 ID
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 총 주문 금액
     */
    long sumTotalAmountByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime startDate, LocalDateTime endDate);
    
    // ========================= 중복 방지 메서드 =========================
    
    /**
     * 멱등성 키로 주문 조회 (중복 방지용)
     * @param idempotencyKey 멱등성 키
     * @return 기존에 생성된 주문 (Optional)
     */
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * 멱등성 키 존재 여부 확인
     * @param idempotencyKey 멱등성 키
     * @return 존재하면 true
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
