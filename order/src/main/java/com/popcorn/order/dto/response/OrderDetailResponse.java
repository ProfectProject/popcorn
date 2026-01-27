package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderStatusHistory;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 상세 정보 응답 DTO
 *
 * 단일 주문의 모든 상세 정보를 담고 있습니다:
 * - 주문 기본 정보
 * - 주문 항목들
 * - 상태 변경 이력
 * - 취소 가능 여부 등 비즈니스 정보
 *
 * 사용 사례:
 * - 주문 상세 페이지
 * - 주문 관리 대시보드
 * - 고객 서비스 조회
 */
@Getter
@Builder
public class OrderDetailResponse {

    /** 주문 ID */
    private final UUID orderId;

    /** 주문 번호 */
    private final String orderNo;

    /** 제목 (백엔드 호환성을 위해) */
    private final String title;

    /** 설명 (백엔드 호환성을 위해) */
    private final String description;

    /** 고객 ID */
    private final Long customerId;

    /** 팝업 ID */
    private final UUID popupId;

    /** 주문 유형 */
    private final String orderType;

    /** 현재 상태 */
    private final String status;

    /** 총 주문 금액 */
    private final Integer totalAmount;

    /** 취소 가능 시한 */
    private final LocalDateTime cancelableUntil;

    /** 주문 생성 시간 */
    private final LocalDateTime createdAt;

    /** 마지막 수정 시간 */
    private final LocalDateTime updatedAt;

    /** 주문 항목 목록 */
    private final List<OrderItemDetailResponse> items;

    /** 상태 변경 이력 */
    private final List<StatusHistoryResponse> statusHistory;

    /** 현재 취소 가능 여부 */
    private final Boolean cancellable;

    /** 주문 진행률 (0-100) */
    private final Integer progressPercentage;

    /**
     * Order 엔티티와 상태 이력으로부터 상세 응답 생성
     */
    public static OrderDetailResponse fromOrder(Order order, List<OrderStatusHistory> statusHistories) {
        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .popupId(order.getPopupId())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .cancelableUntil(order.getCancelableUntil())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(convertOrderItems(order))
                .statusHistory(convertStatusHistory(statusHistories))
                .cancellable(order.isCancelable())
                .progressPercentage(calculateProgressPercentage(order.getStatus().name()))
                .build();
    }

    /**
     * 주문 항목을 응답 DTO로 변환
     */
    private static List<OrderItemDetailResponse> convertOrderItems(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return List.of();
        }

        return order.getOrderItems().stream()
                .map(OrderItemDetailResponse::fromOrderItem)
                .toList();
    }

    /**
     * 상태 이력을 응답 DTO로 변환
     */
    private static List<StatusHistoryResponse> convertStatusHistory(List<OrderStatusHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return List.of();
        }

        return histories.stream()
                .map(StatusHistoryResponse::fromStatusHistory)
                .toList();
    }

    /**
     * 주문 상태에 따른 진행률 계산
     */
    private static Integer calculateProgressPercentage(String status) {
        return switch (status) {
            case "REQUESTED" -> 10;
            case "ACCEPTED" -> 25;
            case "RESERVED" -> 40;
            case "PAYMENT_PENDING" -> 60;
            case "PAID" -> 80;
            case "COMPLETED" -> 100;
            case "CANCELLED", "REJECTED" -> 0;
            default -> 0;
        };
    }

    // ================ 중첩 DTO 클래스들 ================

    /**
     * 주문 항목 상세 응답 DTO
     */
    @Getter
    @Builder
    public static class OrderItemDetailResponse {
        private final UUID itemId;
        private final String orderItemType;
        private final Integer qty;
        private final Integer unitPrice;
        private final Integer lineAmount;
        private final UUID sessionOptionId;
        private final UUID goodsId;

        public static OrderItemDetailResponse fromOrderItem(com.popcorn.order.entity.OrderItem item) {
            return OrderItemDetailResponse.builder()
                    .itemId(item.getId())
                    .orderItemType(item.getOrderItemType().name())
                    .qty(item.getQty())
                    .unitPrice(item.getUnitPrice())
                    .lineAmount(item.getLineAmount())
                    .sessionOptionId(item.getSessionOptionId())
                    .goodsId(item.getGoodsId())
                    .build();
        }
    }

    /**
     * 상태 변경 이력 응답 DTO
     */
    @Getter
    @Builder
    public static class StatusHistoryResponse {
        private final UUID historyId;
        private final String fromStatus;
        private final String toStatus;
        private final String reason;
        private final LocalDateTime changedAt;

        public static StatusHistoryResponse fromStatusHistory(OrderStatusHistory history) {
            return StatusHistoryResponse.builder()
                    .historyId(history.getId())
                    .fromStatus(history.getFromStatus() != null ? history.getFromStatus().name() : null)
                    .toStatus(history.getToStatus().name())
                    .reason(history.getReason())
                    .changedAt(history.getChangedAt())
                    .build();
        }
    }
}
