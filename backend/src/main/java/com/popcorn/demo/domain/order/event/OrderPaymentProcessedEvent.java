package com.popcorn.demo.domain.order.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

/**
 * 주문 결제 처리 이벤트
 *
 * 주문의 결제가 처리되었을 때 발생하는 이벤트
 * 정산, 재고 차감, 영수증 발급 등에 활용
 */
@Getter
public class OrderPaymentProcessedEvent extends BaseOrderEvent {

    private final String paymentMethod;      // 결제 방법
    private final String paymentStatus;     // COMPLETED, FAILED, REFUNDED
    private final Integer paidAmount;       // 결제 금액
    private final Integer discountAmount;   // 할인 금액
    private final String paymentId;         // 외부 결제 시스템 ID
    private final String transactionId;     // 거래 ID
    private final LocalDateTime paymentAt;  // 결제 시점
    private final String paymentProvider;   // 결제 제공업체

    public OrderPaymentProcessedEvent(
            UUID orderId,
            Long userId,
            String paymentMethod,
            String paymentStatus,
            Integer paidAmount,
            Integer discountAmount,
            String paymentId,
            String transactionId,
            String paymentProvider) {

        super(
            orderId,
            "order_payment_processed",
            userId,
            Map.of(
                "paymentMethod", paymentMethod != null ? paymentMethod : "UNKNOWN",
                "paymentStatus", paymentStatus != null ? paymentStatus : "UNKNOWN",
                "paidAmount", paidAmount != null ? paidAmount : 0,
                "discountAmount", discountAmount != null ? discountAmount : 0,
                "paymentId", paymentId != null ? paymentId : "",
                "transactionId", transactionId != null ? transactionId : "",
                "paymentProvider", paymentProvider != null ? paymentProvider : "UNKNOWN",
                "isSuccessfulPayment", isSuccessfulPayment(paymentStatus),
                "hasDiscount", discountAmount != null && discountAmount > 0,
                "paymentRisk", calculatePaymentRisk(paidAmount, paymentMethod)
            )
        );

        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paidAmount = paidAmount != null ? paidAmount : 0;
        this.discountAmount = discountAmount != null ? discountAmount : 0;
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.paymentAt = LocalDateTime.now();
        this.paymentProvider = paymentProvider;
    }

    // ================ Business Logic ================

    /**
     * 결제 성공 여부
     */
    public boolean isSuccessfulPayment() {
        return "COMPLETED".equalsIgnoreCase(paymentStatus) ||
               "SUCCESS".equalsIgnoreCase(paymentStatus);
    }

    /**
     * 결제 실패 여부
     */
    public boolean isFailedPayment() {
        return "FAILED".equalsIgnoreCase(paymentStatus) ||
               "CANCELLED".equalsIgnoreCase(paymentStatus) ||
               "REJECTED".equalsIgnoreCase(paymentStatus);
    }

    /**
     * 환불 처리 여부
     */
    public boolean isRefundPayment() {
        return "REFUNDED".equalsIgnoreCase(paymentStatus) ||
               "REFUND".equalsIgnoreCase(paymentStatus);
    }

    /**
     * 할인이 적용된 결제인지
     */
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount > 0;
    }

    /**
     * 할인율 계산
     */
    public double getDiscountRate() {
        if (!hasDiscount() || paidAmount <= 0) return 0.0;
        int originalAmount = paidAmount + discountAmount;
        return (double) discountAmount / originalAmount * 100;
    }

    /**
     * 고액 결제 여부
     */
    public boolean isHighAmountPayment() {
        return paidAmount >= 100000;
    }

    /**
     * 현금 결제 여부
     */
    public boolean isCashPayment() {
        return "CASH".equalsIgnoreCase(paymentMethod) ||
               "현금".equals(paymentMethod);
    }

    /**
     * 카드 결제 여부
     */
    public boolean isCardPayment() {
        return paymentMethod != null &&
               (paymentMethod.toUpperCase().contains("CARD") ||
                paymentMethod.contains("카드"));
    }

    /**
     * 결제 위험도 평가
     */
    public String getPaymentRisk() {
        return calculatePaymentRisk(paidAmount, paymentMethod);
    }

    /**
     * 정산 우선순위
     */
    public String getSettlementPriority() {
        if (isHighAmountPayment() && isSuccessfulPayment()) return "HIGH";
        if (isFailedPayment() || isRefundPayment()) return "URGENT";
        if (isSuccessfulPayment()) return "NORMAL";
        return "LOW";
    }

    // ================ Event Payload ================

    @Override
    protected Map<String, Object> getEventPayload() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderId", getOrderId());
        payload.put("userId", getUserId());
        payload.put("paymentMethod", paymentMethod != null ? paymentMethod : "");
        payload.put("paymentStatus", paymentStatus != null ? paymentStatus : "");
        payload.put("paidAmount", paidAmount);
        payload.put("discountAmount", discountAmount);
        payload.put("paymentId", paymentId != null ? paymentId : "");
        payload.put("transactionId", transactionId != null ? transactionId : "");
        payload.put("paymentProvider", paymentProvider != null ? paymentProvider : "");
        payload.put("paymentAt", paymentAt);
        payload.put("isSuccessful", isSuccessfulPayment());
        payload.put("hasDiscount", hasDiscount());
        payload.put("discountRate", getDiscountRate());
        payload.put("riskLevel", getPaymentRisk());
        payload.put("settlementPriority", getSettlementPriority());
        return payload;
    }

    /**
     * 결제 상세 설명
     */
    public String getPaymentDescription() {
        String discountText = hasDiscount()
            ? String.format(" (할인: %,d원, %.1f%%)", discountAmount, getDiscountRate())
            : "";

        return String.format(
            "결제 처리: [방법=%s, 상태=%s, 금액=%,d원%s, 제공업체=%s]",
            paymentMethod != null ? paymentMethod : "미상",
            paymentStatus != null ? paymentStatus : "미상",
            paidAmount,
            discountText,
            paymentProvider != null ? paymentProvider : "미상"
        );
    }

    // ================ Helper Methods ================

    private static boolean isSuccessfulPayment(String status) {
        return "COMPLETED".equalsIgnoreCase(status) ||
                "SUCCESS".equalsIgnoreCase(status) ||
                "PAID".equalsIgnoreCase(status);
    }

    private static String calculatePaymentRisk(Integer amount, String method) {
        if (amount == null || amount <= 0) return "UNKNOWN";

        // 고액 현금 결제는 위험도가 높음
        if (amount >= 500000 && "CASH".equalsIgnoreCase(method)) {
            return "HIGH";
        }

        // 고액 결제는 중간 위험
        if (amount >= 200000) {
            return "MEDIUM";
        }

        // 일반 결제는 낮은 위험
        return "LOW";
    }

    @Override
    public String toString() {
        return String.format("OrderPaymentProcessedEvent[orderId=%s, method=%s, status=%s, amount=%d]",
                getOrderId(), paymentMethod, paymentStatus, paidAmount);
    }
}
