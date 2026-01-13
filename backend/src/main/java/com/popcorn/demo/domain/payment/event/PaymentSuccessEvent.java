package com.popcorn.demo.domain.payment.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.order.entity.OrderItem;

import lombok.Builder;
import lombok.Getter;

/**
 * 결제 성공 이벤트
 *
 * 결제 완료 시 발행되어 다양한 후속 작업을 트리거합니다:
 * - 재고 차감
 * - QR 코드 발급
 * - 알림 발송
 * - 로그 기록 등
 */
@Getter
@Builder
public class PaymentSuccessEvent {

	private final UUID orderId;
	private final String orderNo;
	private final UUID paymentId;
	private final String orderType; // RESERVATION or PURCHASE
	private final Integer totalAmount;
	private final Long userId;
	private final List<OrderItem> orderItems;
	private final LocalDateTime paidAt;
	private final String paymentMethod; // "TOSS"
	private final String paymentKey;

	@Override
	public String toString() {
		return String.format("PaymentSuccessEvent(orderNo=%s, paymentId=%s, amount=%d, userId=%d)",
			orderNo, paymentId, totalAmount, userId);
	}
}