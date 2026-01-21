package com.popcorn.order.dto.payment;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Payment 마이크로서비스로부터 받는 결제 생성 응답 DTO
 *
 * - Payment 서비스 → Order 서비스로 오는 응답
 * - 결제가 생성되었는지, 어떤 상태인지 알려줌
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreatePaymentResponse {

    /** 생성된 결제 ID */
    private UUID paymentId;

    /** 결제가 연결된 주문 ID */
    private UUID orderId;

    /** 결제 상태 (PENDING, COMPLETED, FAILED, CANCELLED) */
    private String status;

    /** 결제 금액 */
    private Integer amount;

    /** 결제 방법 */
    private String paymentMethod;

    /** 결제 생성 시간 */
    private LocalDateTime createdAt;

    /** 결제 만료 시간 (결제 대기 시간 제한) */
    private LocalDateTime expiresAt;

    /** 결제 URL (카드 결제, 간편결제 등에서 사용) */
    private String paymentUrl;

    /** 응답 메시지 */
    private String message;

    /** 성공 여부 */
    private boolean success;

    /**
     * 결제가 성공적으로 생성되었는지 확인
     */
    public boolean isPaymentCreated() {
        return success && paymentId != null;
    }

    /**
     * 결제 대기 상태인지 확인
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * 결제 완료 상태인지 확인
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 결제 실패 상태인지 확인
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * 성공적인 결제 응답 생성
     */
    public static CreatePaymentResponse success(UUID paymentId, UUID orderId,
                                              Integer amount, String paymentUrl) {
        return CreatePaymentResponse.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .status("PENDING")
                .amount(amount)
                .paymentUrl(paymentUrl)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30)) // 30분 결제 대기
                .message("결제가 성공적으로 생성되었습니다")
                .success(true)
                .build();
    }

    /**
     * 실패한 결제 응답 생성
     */
    public static CreatePaymentResponse failure(UUID orderId, String errorMessage) {
        return CreatePaymentResponse.builder()
                .orderId(orderId)
                .status("FAILED")
                .createdAt(LocalDateTime.now())
                .message(errorMessage)
                .success(false)
                .build();
    }

}