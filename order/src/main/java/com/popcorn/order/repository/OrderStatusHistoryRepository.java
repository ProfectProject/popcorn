package com.popcorn.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.popcorn.order.entity.OrderStatusHistory;

/**
 * 주문 상태 변경 이력 Repository 인터페이스
 *
 * [초보자 가이드]
 * 주문 상태가 언제, 왜 바뀌었는지를 추적하는 감사(Audit) 목적의 Repository입니다.
 * 모든 상태 변경을 기록해서 문제 발생 시 추적이 가능하도록 합니다.
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    /**
     * 주문별 상태 변경 이력 조회 (최신순)
     *
     * @param orderId 주문 ID
     * @return 상태 변경 이력 목록
     *
     * [초보자 가이드]
     * OrderBy[필드명]Desc: 해당 필드를 기준으로 내림차순 정렬
     * 최신 상태 변경부터 보여줍니다.
     */
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(UUID orderId);

    /**
     * 주문별 상태 변경 이력 조회 (시간순)
     *
     * @param orderId 주문 ID
     * @return 상태 변경 이력 목록 (시간순)
     */
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);

    /**
     * 주문별 최근 상태 변경 이력 조회
     *
     * @param orderId 주문 ID
     * @param limit 조회 개수
     * @return 최근 상태 변경 이력
     *
     * [초보자 가이드]
     * 가장 최근의 상태 변경 몇 건만 조회할 때 사용
     */
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.orderId = :orderId " +
           "ORDER BY h.changedAt DESC LIMIT :limit")
    List<OrderStatusHistory> findRecentHistoriesByOrderId(@Param("orderId") UUID orderId,
                                                          @Param("limit") int limit);

    /**
     * 주문별 상태 변경 횟수 조회
     *
     * @param orderId 주문 ID
     * @return 상태 변경 횟수
     *
     * [초보자 가이드]
     * 해당 주문에 대해 몇 번의 상태 변경이 있었는지 카운트
     */
    long countByOrderId(UUID orderId);

}