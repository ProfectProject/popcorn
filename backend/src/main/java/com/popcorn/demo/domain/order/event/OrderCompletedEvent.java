package com.popcorn.demo.domain.order.event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 완료 이벤트
 *
 * 주문이 성공적으로 완료되었을 때 발생하는 이벤트
 * 정산, 리뷰 요청, 포인트 적립 등 후처리에 활용
 */
public class OrderCompletedEvent extends BaseOrderEvent {

    private final LocalDateTime orderCreatedAt;
    private final LocalDateTime completedAt;
    private final String completedBy;        // 완료 처리한 주체
    private final Integer finalAmount;       // 최종 결제 금액
    private final Integer itemCount;         // 완료된 아이템 수
    private final UUID storeId;             // 상점 ID
    private final Duration processingTime;   // 처리 소요 시간

    public OrderCompletedEvent(
            UUID orderId,
            Long userId,
            UUID storeId,
            LocalDateTime orderCreatedAt,
            String completedBy,
            Integer finalAmount,
            Integer itemCount) {

        super(
            orderId,
            "order_completed",
            userId,
            Map.of(
                "storeId", storeId.toString(),
                "completedBy", completedBy != null ? completedBy : "SYSTEM",
                "finalAmount", finalAmount != null ? finalAmount : 0,
                "itemCount", itemCount != null ? itemCount : 0,
                "orderCreatedAt", orderCreatedAt.toString(),
                "processingTimeMinutes", calculateProcessingMinutes(orderCreatedAt),
                "isQuickService", isQuickService(orderCreatedAt),
                "businessValue", calculateBusinessValue(finalAmount)
            )
        );

        this.orderCreatedAt = orderCreatedAt;
        this.completedAt = LocalDateTime.now();
        this.completedBy = completedBy != null ? completedBy : "SYSTEM";
        this.finalAmount = finalAmount != null ? finalAmount : 0;
        this.itemCount = itemCount != null ? itemCount : 0;
        this.storeId = storeId;
        this.processingTime = Duration.between(orderCreatedAt, completedAt);
    }

    // ================ Getters ================

    public LocalDateTime getOrderCreatedAt() { return orderCreatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getCompletedBy() { return completedBy; }
    public Integer getFinalAmount() { return finalAmount; }
    public Integer getItemCount() { return itemCount; }
    public UUID getStoreId() { return storeId; }
    public Duration getProcessingTime() { return processingTime; }

    // ================ Business Logic ================

    /**
     * 빠른 서비스 여부 (30분 이내 완료)
     */
    public boolean isQuickService() {
        return processingTime.toMinutes() <= 30;
    }

    /**
     * 처리 시간이 표준 범위 내인지 확인
     */
    public boolean isWithinStandardTime() {
        long minutes = processingTime.toMinutes();
        return minutes >= 15 && minutes <= 60; // 15-60분이 표준
    }

    /**
     * 지연 서비스 여부 (90분 초과)
     */
    public boolean isDelayedService() {
        return processingTime.toMinutes() > 90;
    }

    /**
     * 고가 주문 여부
     */
    public boolean isHighValueOrder() {
        return finalAmount >= 50000;
    }

    /**
     * 리뷰 요청 우선순위
     */
    public String getReviewRequestPriority() {
        if (isHighValueOrder() && isQuickService()) return "HIGH";
        if (isHighValueOrder() || isQuickService()) return "MEDIUM";
        if (isDelayedService()) return "LOW";
        return "NORMAL";
    }

    /**
     * 포인트 적립률 계산 (금액 기반)
     */
    public double getPointAccrualRate() {
        if (finalAmount >= 100000) return 0.03;  // 3%
        if (finalAmount >= 50000) return 0.02;   // 2%
        return 0.01;  // 1%
    }

    /**
     * 적립 예정 포인트
     */
    public int getAccrualPoints() {
        return (int) (finalAmount * getPointAccrualRate());
    }

    // ================ Event Payload ================

    @Override
    protected Map<String, Object> getEventPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderId", getOrderId());
        payload.put("userId", getUserId());
        payload.put("storeId", storeId);
        payload.put("completedBy", completedBy);
        payload.put("finalAmount", finalAmount);
        payload.put("itemCount", itemCount);
        payload.put("orderCreatedAt", orderCreatedAt);
        payload.put("completedAt", completedAt);
        payload.put("processingTimeMinutes", processingTime.toMinutes());
        payload.put("isQuickService", isQuickService());
        payload.put("isHighValue", isHighValueOrder());
        payload.put("reviewPriority", getReviewRequestPriority());
        payload.put("accrualPoints", getAccrualPoints());
        return payload;
    }

    /**
     * 완료 상세 설명
     */
    public String getCompletionDescription() {
        return String.format(
            "주문 완료: [금액=%,d원, 아이템=%d개, 처리시간=%d분, 포인트=%d점]",
            finalAmount,
            itemCount,
            processingTime.toMinutes(),
            getAccrualPoints()
        );
    }

    /**
     * 서비스 품질 평가
     */
    public String getServiceQuality() {
        if (isQuickService() && !isDelayedService()) return "EXCELLENT";
        if (isWithinStandardTime()) return "GOOD";
        if (isDelayedService()) return "NEEDS_IMPROVEMENT";
        return "FAIR";
    }

    // ================ Helper Methods ================

    private static long calculateProcessingMinutes(LocalDateTime orderCreatedAt) {
        return Duration.between(orderCreatedAt, LocalDateTime.now()).toMinutes();
    }

    private static boolean isQuickService(LocalDateTime orderCreatedAt) {
        return calculateProcessingMinutes(orderCreatedAt) <= 30;
    }

    private static String calculateBusinessValue(Integer amount) {
        if (amount == null || amount <= 0) return "NONE";
        if (amount >= 100000) return "PREMIUM";
        if (amount >= 50000) return "HIGH";
        if (amount >= 20000) return "MEDIUM";
        return "STANDARD";
    }

    @Override
    public String toString() {
        return String.format("OrderCompletedEvent[orderId=%s, amount=%d, time=%dmin]",
                getOrderId(), finalAmount, processingTime.toMinutes());
    }
}