package com.popcorn.demo.domain.payment.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;

import lombok.Getter;

/**
 * 결제 생성 이벤트
 * 결제가 최초 생성될 때 발생하는 이벤트
 */
@Getter
public class PaymentCreatedEvent extends ApplicationEvent {

	private final UUID paymentId;
	private final UUID orderId;
	private final PaymentMethod method;
	private final PaymentStatus status;
	private final Integer amount;
	private final String orderType;
	private final Long customerId;
	private final LocalDateTime createdAt;

	public PaymentCreatedEvent(
			Object source,
			UUID paymentId,
			UUID orderId,
			PaymentMethod method,
			PaymentStatus status,
			Integer amount,
			OrderType orderType,
			Long customerId,
			LocalDateTime createdAt) {
		super(source);
		this.paymentId = paymentId;
		this.orderId = orderId;
		this.method = method;
		this.status = status;
		this.amount = amount;
		this.orderType = orderType != null ? orderType.name() : "PURCHASE";
		this.customerId = customerId;
		this.createdAt = createdAt;
	}
}