package com.popcorn.demo.domain.payment.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;

import lombok.Getter;

/**
 * 결제 실패 이벤트
 * 결제가 실패할 때 발생하는 이벤트
 * 이 이벤트를 통해 임시 예약된 재고 복원, 알림 등이 처리됨
 */
@Getter
public class PaymentFailedEvent extends ApplicationEvent {

	private final UUID paymentId;
	private final UUID orderId;
	private final PaymentMethod method;
	private final Integer amount;
	private final String orderType;
	private final Long customerId;
	private final LocalDateTime failedAt;

	public PaymentFailedEvent(
			Object source,
			UUID paymentId,
			UUID orderId,
			PaymentMethod method,
			Integer amount,
			OrderType orderType,
			Long customerId,
			LocalDateTime failedAt) {
		super(source);
		this.paymentId = paymentId;
		this.orderId = orderId;
		this.method = method;
		this.amount = amount;
		this.orderType = orderType != null ? orderType.name() : "PURCHASE";
		this.customerId = customerId;
		this.failedAt = failedAt;
	}
}