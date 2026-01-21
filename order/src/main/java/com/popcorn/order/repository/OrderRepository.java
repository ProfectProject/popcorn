package com.popcorn.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderStatus;

/**
 * 주문 Repository 인터페이스
 *
 * [초보자 가이드]
 * Repository 패턴: 데이터 액세스 로직을 캡슐화하는 패턴
 *
 * JpaRepository를 확장하면 기본적인 CRUD 메서드들이 자동으로 제공됩니다:
 * - save(entity): 엔티티 저장/수정
 * - findById(id): ID로 엔티티 조회
 * - findAll(): 모든 엔티티 조회
 * - delete(entity): 엔티티 삭제
 * - count(): 전체 개수 조회 등
 *
 * 추가로 필요한 쿼리 메서드들을 정의할 수 있습니다.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * 주문 번호로 주문 조회
     *
     * @param orderNo 주문 번호
     * @return 주문 정보
     *
     * [초보자 가이드]
     * 메서드 네이밍 규칙:
     * - findBy[필드명]: 해당 필드로 검색
     * - Optional<T>: 결과가 없을 수도 있음을 명시
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 사용자별 주문 목록 조회
     *
     * @param customerId 고객 ID
     * @return 주문 목록
     *
     * [초보자 가이드]
     * 자동 생성되는 쿼리: SELECT * FROM orders WHERE customer_id = ?
     */
    List<Order> findByCustomerId(Long customerId);

    /**
     * 주문 상태별 조회
     *
     * @param status 주문 상태
     * @return 주문 목록
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * 사용자별 특정 상태 주문 조회
     *
     * @param customerId 고객 ID
     * @param status 주문 상태
     * @return 주문 목록
     *
     * [초보자 가이드]
     * 복합 조건 검색: And로 여러 조건을 연결
     * 자동 생성 쿼리: SELECT * FROM orders WHERE customer_id = ? AND status = ?
     */
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    /**
     * 사용자별 주문 개수 조회
     *
     * @param customerId 고객 ID
     * @return 주문 개수
     */
    long countByCustomerId(Long customerId);

    /**
     * 사용자별 최근 주문 조회 (페이징)
     *
     * @param customerId 고객 ID
     * @param limit 조회 개수
     * @return 최근 주문 목록
     *
     * [초보자 가이드]
     * @Query: 직접 JPQL(또는 SQL)을 작성할 때 사용
     * ORDER BY ... DESC: 최신순 정렬
     * LIMIT: 조회 개수 제한 (MySQL 문법, PostgreSQL에서는 다를 수 있음)
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId " +
           "ORDER BY o.createdAt DESC LIMIT :limit")
    List<Order> findRecentOrdersByCustomerId(@Param("customerId") Long customerId,
                                           @Param("limit") int limit);

    /**
     * 취소 가능한 주문 목록 조회
     *
     * @return 취소 가능한 주문 목록
     *
     * [초보자 가이드]
     * CURRENT_TIMESTAMP: 현재 시각
     * 취소 가능 시간이 현재 시각보다 이후이고, 상태가 취소/완료가 아닌 주문들
     */
    @Query("SELECT o FROM Order o WHERE o.cancelableUntil > CURRENT_TIMESTAMP " +
           "AND o.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<Order> findCancelableOrders();

    // ================ 페이징 지원 메서드들 ================

    /**
     * 사용자별 주문 목록 조회 (페이징, 최신순)
     */
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    /**
     * 팝업별 주문 목록 조회 (페이징, 최신순)
     */
    Page<Order> findByPopupIdOrderByCreatedAtDesc(UUID popupId, Pageable pageable);

    /**
     * 주문 번호 존재 여부 확인
     */
    boolean existsByOrderNo(String orderNo);

    // ================ 커스텀 쿼리 메서드들 ================

    /**
     * 취소 가능한 주문들 조회 (특정 사용자, 페이징)
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId " +
           "AND o.cancelableUntil > :currentTime " +
           "AND o.status NOT IN ('CANCELLED', 'COMPLETED', 'REJECTED') " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findCancellableOrdersByCustomerId(
            @Param("customerId") Long customerId,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);

    /**
     * 오래된 요청 상태 주문들 조회 (관리자용)
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'REQUESTED' " +
           "AND o.createdAt < :cutoffTime " +
           "ORDER BY o.createdAt ASC")
    Page<Order> findRequestedOrdersOlderThan(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);

}
