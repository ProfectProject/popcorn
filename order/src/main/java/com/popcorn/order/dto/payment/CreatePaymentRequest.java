package com.popcorn.order.dto.payment;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Payment 마이크로서비스에게 보내는 결제 생성 요청 DTO
 *
 * - Order → Payment 서비스로 보내는 데이터
 * - 주문 정보를 바탕으로 결제를 시작함
 */
@Getter
@Builder
@AllArgsConstructor
@ToString
public class CreatePaymentRequest {

    /** 결제할 주문 ID */
    @NotNull(message = "주문 ID는 필수입니다")
    private final UUID orderId;

    /** 주문한 고객 ID */
    @NotNull(message = "고객 ID는 필수입니다")
    private final Long customerId;

    /** 결제할 금액 (원) */
    @NotNull(message = "결제 금액은 필수입니다")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다")
    private final Integer amount;

    /** 결제 방법 (CARD, BANK_TRANSFER, KAKAO_PAY 등) */
    @NotNull(message = "결제 방법은 필수입니다")
    private final String paymentMethod;

    /** 주문 번호 (사용자에게 보이는) */
    private final String orderNo;

    /** 상품명 (결제창에 표시될) */
    private final String itemName;

    /** 고객 이름 */
    private final String customerName;

    /** 고객 이메일 */
    private final String customerEmail;

    /** 고객 전화번호 */
    private final String customerPhone;

    /**
     * Order 엔티티로부터 결제 요청 생성하기
     *
     * [초보자 가이드]
     * - Order 정보 → Payment 요청으로 변환
     * - 필요한 정보만 선별해서 전달
     */
    public static CreatePaymentRequest fromOrder(
            UUID orderId,
            Long customerId,
            String orderNo,
            Integer totalAmount,
            String paymentMethod) {

        return CreatePaymentRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .orderNo(orderNo)
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .itemName("팝콘 팝업 주문 - " + orderNo)
                .build();
    }

    /**
     * 고객 정보 추가 설정
     */
    public CreatePaymentRequest withCustomerInfo(String name, String email, String phone) {
        return CreatePaymentRequest.builder()
                .orderId(this.orderId)
                .customerId(this.customerId)
                .amount(this.amount)
                .paymentMethod(this.paymentMethod)
                .orderNo(this.orderNo)
                .itemName(this.itemName)
                .customerName(name)
                .customerEmail(email)
                .customerPhone(phone)
                .build();
    }

}