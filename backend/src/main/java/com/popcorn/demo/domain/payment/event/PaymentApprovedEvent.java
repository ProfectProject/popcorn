package com.popcorn.demo.domain.payment.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;

import lombok.Getter;

/**
 * 결제 승인 이벤트
 * 결제가 성공적으로 승인될 때 발생하는 이벤트
 * 이 이벤트를 통해 재고 차감, QR 코드 생성, 알림 등이 처리됨
 */
@Getter
public class PaymentApprovedEvent extends ApplicationEvent {

	private final UUID paymentId;
	private final UUID orderId;
	private final PaymentMethod method;
	private final Integer amount;
	private final String orderType;
	private final Long customerId;
	private final LocalDateTime approvedAt;

	public PaymentApprovedEvent(
			Object source,
			UUID paymentId,
			UUID orderId,
			PaymentMethod method,
			Integer amount,
			OrderType orderType,
			Long customerId,
			LocalDateTime approvedAt) {
		super(source);
		this.paymentId = paymentId;
		this.orderId = orderId;
		this.method = method;
		this.amount = amount;
		this.orderType = orderType != null ? orderType.name() : "PURCHASE";
		this.customerId = customerId;
		this.approvedAt = approvedAt;
	}
}