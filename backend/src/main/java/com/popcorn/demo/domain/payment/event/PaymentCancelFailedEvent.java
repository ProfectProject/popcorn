package com.popcorn.demo.domain.payment.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * 결제 취소 실패 이벤트
 * 토스 결제 취소가 실패했을 때 발생하는 이벤트
 * 실패 큐에 저장되어 재시도 처리됨
 */
@Getter
public class PaymentCancelFailedEvent extends ApplicationEvent {

	private final UUID orderId;
	private final UUID paymentId;
	private final String paymentKey;
	private final String cancelReason;
	private final String failureReason;
	private final Integer amount;
	private final LocalDateTime failedAt;
	private final int attemptCount;

	public PaymentCancelFailedEvent(
			Object source,
			UUID orderId,
			UUID paymentId,
			String paymentKey,
			String cancelReason,
			String failureReason,
			Integer amount,
			LocalDateTime failedAt,
			int attemptCount) {
		super(source);
		this.orderId = orderId;
		this.paymentId = paymentId;
		this.paymentKey = paymentKey;
		this.cancelReason = cancelReason;
		this.failureReason = failureReason;
		this.amount = amount;
		this.failedAt = failedAt;
		this.attemptCount = attemptCount;
	}
}