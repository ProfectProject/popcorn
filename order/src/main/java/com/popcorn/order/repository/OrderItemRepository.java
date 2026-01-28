package com.popcorn.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.popcorn.order.entity.OrderItem;

/**
 * 주문 항목 Repository 인터페이스
 *
 * [초보자 가이드]
 * OrderItem은 주문에 포함된 각각의 상품 정보를 관리합니다.
 * 예: 팝콘 3개, 음료 2개가 있는 주문의 경우 2개의 OrderItem이 생성됩니다.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * 주문별 주문 항목 조회
     *
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     *
     * [초보자 가이드]
     * 특정 주문에 포함된 모든 상품들을 조회합니다.
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * 팝업별 주문 항목 조회
     *
     * @param popupId 팝업 ID
     * @return 주문 항목 목록
     */
    List<OrderItem> findByPopupId(UUID popupId);

    /**
     * 주문별 총 수량 계산
     *
     * @param orderId 주문 ID
     * @return 총 수량
     *
     * [초보자 가이드]
     * SUM 함수를 사용해서 해당 주문의 모든 항목 수량을 합산합니다.
     * COALESCE: NULL인 경우 0으로 반환
     */
    @Query("SELECT COALESCE(SUM(oi.qty), 0) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Integer getTotalQuantityByOrderId(@Param("orderId") UUID orderId);

    /**
     * 주문별 총 금액 계산
     *
     * @param orderId 주문 ID
     * @return 총 금액
     */
    @Query("SELECT COALESCE(SUM(oi.lineAmount), 0) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Integer getTotalAmountByOrderId(@Param("orderId") UUID orderId);

    /**
     * 스케줄별 주문 항목 조회 (예약형 상품)
     *
     * @param sessionOptionId 세션 옵션 ID
     * @return 주문 항목 목록
     */
    List<OrderItem> findBySessionOptionId(UUID sessionOptionId);

    /**
     * 굿즈 변형별 주문 항목 조회 (구매형 상품)
     *
     * @param goodsId 굿즈 변형 ID
     * @return 주문 항목 목록
     */
    List<OrderItem> findByGoodsId(UUID goodsId);

    /**
     * 주문 항목 타입별 조회
     *
     * @param orderId 주문 ID
     * @param orderItemType 주문 항목 타입
     * @return 주문 항목 목록
     *
     * [초보자 가이드]
     * 특정 주문에서 예약형 상품만 또는 굿즈형 상품만 조회할 때 사용
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId " +
           "AND oi.orderItemType = :orderItemType")
    List<OrderItem> findByOrderIdAndItemType(@Param("orderId") UUID orderId,
                                                 @Param("orderItemType") String orderItemType);

    /**
     * 주문별 항목 개수 조회
     *
     * @param orderId 주문 ID
     * @return 항목 개수
     */
    long countByOrderId(UUID orderId);

}
