package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.order.entity.Order;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 요약 정보 응답 DTO
 *
 * 주문 목록에서 사용되는 간단한 요약 정보만 포함:
 * - 주문 번호, 상태, 금액 등 핵심 정보만
 * - 상세 정보는 별도 조회 필요
 * - 성능 최적화를 위해 최소한의 정보만 포함
 *
 * 사용 사례:
 * - 주문 목록 페이지
 * - 대시보드 주문 요약
 * - 검색 결과 목록
 */
@Getter
@Builder
public class OrderSummaryResponse {

    /** 주문 ID */
    private final UUID orderId;

    /** 주문 번호 */
    private final String orderNo;

    /** 고객 ID */
    private final Long customerId;

    /** 팝업 ID */
    private final UUID popupId;

    /** 주문 유형 */
    private final String orderType;

    /** 현재 상태 */
    private final String status;

    /** 상태 표시명 (한국어) */
    private final String statusDisplayName;

    /** 총 주문 금액 */
    private final Integer totalAmount;

    /** 주문 항목 수 */
    private final Integer itemCount;

    /** 취소 가능 여부 */
    private final Boolean cancellable;

    /** 주문 생성 시간 */
    private final LocalDateTime createdAt;

    /** 마지막 수정 시간 */
    private final LocalDateTime updatedAt;

    /** 급한 주문인지 여부 (취소 시한이 임박한 주문) */
    private final Boolean urgent;

    /**
     * Order 엔티티로부터 요약 응답 생성
     */
    public static OrderSummaryResponse fromOrder(Order order) {
        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .popupId(order.getPopupId())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .statusDisplayName(getStatusDisplayName(order.getStatus().name()))
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .cancellable(order.isCancelable())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .urgent(isUrgentOrder(order))
                .build();
    }

    /**
     * 상태의 한국어 표시명 반환
     */
    private static String getStatusDisplayName(String status) {
        return switch (status) {
            case "REQUESTED" -> "주문 요청됨";
            case "ACCEPTED" -> "주문 수락됨";
            case "REJECTED" -> "주문 거절됨";
            case "RESERVED" -> "예약 확정됨";
            case "PAYMENT_PENDING" -> "결제 대기";
            case "PAID" -> "결제 완료";
            case "COMPLETED" -> "주문 완료";
            case "CANCELLED" -> "취소됨";
            default -> status;
        };
    }

    /**
     * 급한 주문인지 확인 (취소 가능 시한이 1시간 이내)
     */
    private static Boolean isUrgentOrder(Order order) {
        if (order.getCancelableUntil() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        return order.getCancelableUntil().isBefore(oneHourLater);
    }

    // ================ 편의 메서드들 ================

    /**
     * 완료된 주문인지 확인
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 취소된 주문인지 확인
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    /**
     * 진행 중인 주문인지 확인
     */
    public boolean isInProgress() {
        return !isCompleted() && !isCancelled() && !"REJECTED".equals(status);
    }

    /**
     * 결제 관련 상태인지 확인
     */
    public boolean isPaymentRelated() {
        return "PAYMENT_PENDING".equals(status) || "PAID".equals(status);
    }

    /**
     * 고액 주문인지 확인 (10만원 이상)
     */
    public boolean isHighValue() {
        return totalAmount != null && totalAmount >= 100000;
    }

    /**
     * 예약형 주문인지 확인
     */
    public boolean isReservation() {
        return "RESERVATION".equals(orderType);
    }

    /**
     * 구매형 주문인지 확인
     */
    public boolean isPurchase() {
        return "PURCHASE".equals(orderType);
    }
}
