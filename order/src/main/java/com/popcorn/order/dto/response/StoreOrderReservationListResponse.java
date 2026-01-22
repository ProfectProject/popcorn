package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 주문 목록 조회 응답
 *
 * [Java 초보자를 위한 가이드]
 *
 * 이 클래스가 사용되는 곳:
 * 1. GET /api/orders/v1/store - 매장별 주문 현황
 * 2. GET /api/orders/v1/popup/{id}/reservations - 팝업 예약 목록
 * 3. GET /api/orders/v1/popup/{id}/purchases - 팝업 구매 목록
 *
 * 왜 이름이 "Reservation"인가?
 * - 처음에는 예약 주문만 처리하려고 만든 클래스
 * - 나중에 구매 주문도 같은 구조로 처리하게 되어 재사용
 * - 실제로는 예약/구매 모든 주문 타입을 다룸
 *
 * 매장 운영자가 보는 정보:
 * - 고객들의 주문 목록 (상태, 금액, 취소 가능 시한 등)
 * - 매장/팝업별로 필터링된 결과
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderReservationListResponse {

    private List<ItemDto> items;
    private int page;
    private int size;
    private long total;

    /**
     * 개별 주문 항목 정보 (매장 운영자용)
     *
     * [고객용 vs 매장용 차이점]
     * - 고객용(MyOrderTimelineResponse): 팝업 정보, 위치 정보 포함
     * - 매장용(이 클래스): 주문 번호, 금액, 상태 중심의 간단한 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private UUID id;                        // 주문 ID
        private String reservationNo;           // 예약 번호 (또는 주문 번호)
        private String status;                  // 주문 상태
        private Integer totalAmount;            // 총 주문 금액
        private LocalDateTime cancelableUntil;  // 취소 가능 시한
        private LocalDateTime createdAt;        // 주문 생성 시각
    }
}