package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 주문 타임라인 조회 응답
 *
 * [Java 초보자를 위한 가이드]
 *
 * 이 클래스가 하는 일:
 * - 고객이 "내 주문 목록"을 조회했을 때 반환되는 데이터 구조
 * - 예약 주문과 구매 주문을 모두 포함하여 시간순으로 정렬된 목록
 *
 * 포함된 정보:
 * - items: 주문 목록 (예약/구매 구분, 상태, 금액, 위치 등)
 * - page/size/total: 페이지네이션 정보
 *
 * 사용 예시:
 * GET /api/orders/v1/me?page=1&size=20
 * → 내 주문 20개씩 1페이지 조회
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyOrderTimelineResponse {

    private List<ItemDto> items;
    private int page;
    private int size;
    private long total;

    /**
     * 개별 주문 항목 정보
     *
     * [Java 초보자 설명]
     * static class란?
     * - 바깥 클래스(MyOrderTimelineResponse) 안에 정의된 내부 클래스
     * - 독립적으로 사용 가능 (바깥 클래스 인스턴스 없이도 생성 가능)
     * - JSON 응답에서 중첩된 객체 구조를 표현할 때 사용
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private String type;                    // 주문 타입 ("RESERVATION" 또는 "PURCHASE")
        private UUID id;                        // 주문 ID
        private String orderNo;                 // 주문 번호 (고객이 보는 식별자)
        private String status;                  // 주문 상태 (REQUESTED, PAID, COMPLETED 등)
        private Integer totalAmount;            // 총 주문 금액 (원 단위)
        private LocalDateTime cancelableUntil;  // 취소 가능 시한
        private LocalDateTime createdAt;        // 주문 생성 시각
        private UUID popupId;                   // 팝업 ID (어느 팝업에서 주문했는지)
        private UUID storeId;                   // 매장 ID
        private String title;                   // 팝업 또는 상품 제목
        private LocalDateTime sessionStartAt;   // 세션 시작 시각 (예약 주문인 경우)
        private LocationDto location;           // 매장 위치 정보
    }

    /**
     * 매장 위치 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private String name;        // 매장명
        private String address1;    // 기본 주소
        private String address2;    // 상세 주소
    }
}