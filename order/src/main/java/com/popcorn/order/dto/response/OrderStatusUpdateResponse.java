package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 상태 변경 응답 DTO
 *
 * 주문 상태 변경 처리 결과를 반환합니다.
 */
@Getter
@Builder
public class OrderStatusUpdateResponse {

    /** 상태가 변경된 주문 ID */
    private final UUID orderId;

    /** 팝업 ID (백엔드 호환성을 위해) */
    private final UUID popupId;

    /** 주문 번호 */
    private final String orderNo;

    /** 이전 상태 */
    private final String previousStatus;

    /** 현재 상태 */
    private final String currentStatus;

    /** 백엔드 호환성을 위한 상태 */
    private final String status;

    /** 상태 변경 시간 */
    private final LocalDateTime updatedAt;

    /** 상태 변경 사유 */
    private final String reason;

    /** 상태 변경 주체 */
    private final String updatedBy;

    /** 상태 변경 성공 여부 */
    private final Boolean success;

    /** 처리 메시지 */
    private final String message;

    /** 고객 알림 발송 여부 */
    private final Boolean customerNotified;

    /** 다음 가능한 상태들 */
    private final java.util.List<String> nextAvailableStatuses;

    /** 주문 진행률 (0-100) */
    private final Integer progressPercentage;

    /** 예상 완료 시간 (상태에 따라) */
    private final LocalDateTime estimatedCompletionTime;

    /**
     * 성공적인 상태 변경 응답 생성
     */
    public static OrderStatusUpdateResponse success(
            UUID orderId,
            String orderNo,
            String previousStatus,
            String currentStatus,
            String reason,
            String updatedBy) {
        return OrderStatusUpdateResponse.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .updatedAt(LocalDateTime.now())
                .reason(reason)
                .updatedBy(updatedBy)
                .success(true)
                .message("주문 상태가 성공적으로 변경되었습니다.")
                .customerNotified(true)
                .progressPercentage(calculateProgressPercentage(currentStatus))
                .build();
    }

    /**
     * 실패한 상태 변경 응답 생성
     */
    public static OrderStatusUpdateResponse failure(
            UUID orderId,
            String orderNo,
            String currentStatus,
            String errorMessage) {
        return OrderStatusUpdateResponse.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .currentStatus(currentStatus)
                .success(false)
                .message(errorMessage)
                .customerNotified(false)
                .build();
    }

    /**
     * 상태별 진행률 계산
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
}