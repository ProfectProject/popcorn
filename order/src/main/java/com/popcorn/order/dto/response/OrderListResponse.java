package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 목록 응답 DTO
 *
 * 주문 목록 조회 시 페이징된 주문 정보들을 반환합니다.
 */
@Getter
@Builder
public class OrderListResponse {

    /** 주문 목록 */
    private final List<OrderSummary> orders;

    /** 백엔드 호환성을 위한 항목 목록 */
    private final List<OrderSummary> items;

    /** 페이징 정보 */
    private final PageInfo pageInfo;

    /** 백엔드 호환성을 위한 페이지 번호 */
    private final Integer page;

    /** 백엔드 호환성을 위한 페이지 크기 */
    private final Integer size;

    /** 백엔드 호환성을 위한 총 개수 */
    private final Long total;

    /** 목록 요약 정보 */
    private final ListSummary summary;

    /** 백엔드 호환성을 위한 total getter */
    public Long getTotal() {
        return pageInfo != null ? pageInfo.getTotalElements() : 0L;
    }

    /** 백엔드 호환성을 위한 items getter */
    public List<OrderSummary> getItems() {
        return orders;
    }

    /**
     * 페이징 정보
     */
    @Getter
    @Builder
    public static class PageInfo {
        private final Integer currentPage;
        private final Integer pageSize;
        private final Long totalElements;
        private final Integer totalPages;
        private final Boolean hasNext;
        private final Boolean hasPrevious;
    }

    /**
     * 목록 요약 정보
     */
    @Getter
    @Builder
    public static class ListSummary {
        private final Long totalOrders;
        private final Integer totalAmount;
        private final java.util.Map<String, Long> statusCounts;
        private final LocalDateTime lastUpdated;
    }

    /**
     * 주문 요약 정보
     */
    @Getter
    @Builder
    public static class OrderSummary {
        /** 주문 ID */
        private final UUID orderId;

        /** 주문 번호 */
        private final String orderNo;

        /** 고객 ID */
        private final Long customerId;

        /** 고객 이름 (조회 가능한 경우) */
        private final String customerName;

        /** 스토어 이름 */
        private final String storeName;

        /** 팝업 ID */
        private final UUID popupId;

        /** 팝업 이름 */
        private final String popupName;

        /** 주문 유형 */
        private final String orderType;

        /** 현재 상태 */
        private final String status;

        /** 상태 표시명 */
        private final String statusDisplayName;

        /** 총 주문 금액 */
        private final Integer totalAmount;

        /** 주문 항목 개수 */
        private final Integer itemCount;

        /** 대표 상품명 (첫 번째 항목) */
        private final String representativeItemName;

        /** 취소 가능 여부 */
        private final Boolean cancellable;

        /** 취소 가능 시한 */
        private final LocalDateTime cancelableUntil;

        /** 주문 생성 시간 */
        private final LocalDateTime createdAt;

        /** 마지막 수정 시간 */
        private final LocalDateTime updatedAt;

        /** 주문 진행률 (0-100) */
        private final Integer progressPercentage;

        /** 긴급 주문 여부 */
        private final Boolean urgent;

        /** 특별 요청사항 여부 */
        private final Boolean hasSpecialRequest;
    }

    /**
     * 빈 목록 응답 생성
     */
    public static OrderListResponse empty(Integer page, Integer size) {
        return OrderListResponse.builder()
                .orders(List.of())
                .pageInfo(PageInfo.builder()
                        .currentPage(page)
                        .pageSize(size)
                        .totalElements(0L)
                        .totalPages(0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build())
                .summary(ListSummary.builder()
                        .totalOrders(0L)
                        .totalAmount(0)
                        .statusCounts(java.util.Map.of())
                        .lastUpdated(LocalDateTime.now())
                        .build())
                .build();
    }

    /**
     * 상태별 표시명 반환
     */
    public static String getStatusDisplayName(String status) {
        return switch (status) {
            case "REQUESTED" -> "주문 접수";
            case "ACCEPTED" -> "주문 승인";
            case "RESERVED" -> "예약 확정";
            case "PAYMENT_PENDING" -> "결제 대기";
            case "PAID" -> "결제 완료";
            case "COMPLETED" -> "주문 완료";
            case "CANCELLED" -> "취소됨";
            case "REJECTED" -> "거절됨";
            default -> status;
        };
    }
}
