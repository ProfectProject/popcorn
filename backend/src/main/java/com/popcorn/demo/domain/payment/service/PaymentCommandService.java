package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.event.PaymentApprovedEvent;
import com.popcorn.demo.domain.payment.event.PaymentCancelledEvent;
import com.popcorn.demo.domain.payment.event.PaymentCreatedEvent;
import com.popcorn.demo.domain.payment.event.PaymentFailedEvent;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

	private static final Logger log = LoggerFactory.getLogger(PaymentCommandService.class);

	private final OrderRepository orderRepository;
	private final OrderCommandService orderCommandService;
	private final JpaPaymentRepository paymentRepository;
	private final JpaOrderItemRepository orderItemRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId);
		PaymentMethod paymentMethod = parseMethod(method);
		OrderType orderType = resolveOrderType(orderId);
		validateMethodByOrderType(orderType, paymentMethod);
		PaymentCreationResult result = createPayment(order, paymentMethod, amount, rawPayload, PaymentStatus.READY, null);

		// 결제 생성 이벤트 발행
		eventPublisher.publishEvent(new PaymentCreatedEvent(
				this,
				result.getPaymentId(),
				orderId,
				paymentMethod,
				PaymentStatus.READY,
				amount,
				orderType,
				order.getCustomerId(),
				LocalDateTime.now()
		));

		return result;
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentDetailResult updatePaymentStatus(UUID paymentId, String status) {
		return updatePaymentStatus(paymentId, status, null, null);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentDetailResult updatePaymentStatus(UUID paymentId, String status, String rawPayload, LocalDateTime approvedAt) {
		PaymentStatus targetStatus = parseStatus(status);
		Payment payment = loadPayment(paymentId);

		return switch (targetStatus) {
			case PAID -> approvePayment(payment, rawPayload, approvedAt);
			case FAILED -> failPayment(payment);
			case CANCELLED -> cancelPayment(payment);
			default -> throw PaymentException.invalidRequest();
		};
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public void deletePayment(UUID paymentId) {
		Payment payment = loadPayment(paymentId);
		payment.setDeletedAt(LocalDateTime.now());
		paymentRepository.save(payment);
	}


	private Order loadOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderNotFoundException::orderNotFound);
		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw OrderValidationException.invalidStatusTransition();
		}
		if (paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)) {
			throw PaymentException.paymentAlreadyExists();
		}
		return order;
	}

	private PaymentCreationResult createPayment(
			Order order,
			PaymentMethod method,
			Integer amount,
			String rawPayload,
			PaymentStatus paymentStatus,
			LocalDateTime approvedAt) {
		if (amount == null || amount <= 0) {
			throw PaymentException.invalidRequest();
		}

		Payment payment = Payment.builder()
				.orderId(order.getId())
				.method(method)
				.status(paymentStatus)
				.amount(amount)
				.rawPayload(rawPayload)
				.approvedAt(approvedAt)
				.build();

		Payment saved = paymentRepository.save(payment);
		OrderStatus orderStatus = order.getStatus();
		if (orderStatus != OrderStatus.PAYMENT_PENDING) {
			orderStatus = updateOrderStatus(order.getId(), OrderStatus.PAYMENT_PENDING, "결제 대기");
		}

		return PaymentCreationResult.builder()
				.paymentId(saved.getId())
				.paymentStatus(paymentStatus)
				.orderStatus(orderStatus)
				.approvedAt(approvedAt)
				.orderNo(order.getOrderNo())
				.amount(saved.getAmount())
				.customerId(order.getCustomerId())
				.build();
	}

	private PaymentMethod parseMethod(String method) {
		if (method == null || method.isBlank()) {
			throw PaymentException.invalidRequest();
		}
		try {
			return PaymentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw PaymentException.invalidRequest();
		}
	}

	private void validateReservationMethod(PaymentMethod method) {
		if (method != PaymentMethod.CARD
				&& method != PaymentMethod.TRANSFER
				&& method != PaymentMethod.EASY_PAY) {
			throw PaymentException.invalidRequest();
		}
	}

	private OrderType resolveOrderType(UUID orderId) {
		boolean hasSchedule = orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId);
		boolean hasGoods = orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId);

		// 혼합 주문의 경우 예약이 있으면 RESERVATION 타입으로 처리 (예약이 더 시간에 민감함)
		if (hasSchedule) {
			return OrderType.RESERVATION;
		}
		if (hasGoods) {
			return OrderType.PURCHASE;
		}
		throw PaymentException.invalidRequest();
	}

	private void validateMethodByOrderType(OrderType orderType, PaymentMethod method) {
		if (orderType == OrderType.RESERVATION) {
			validateReservationMethod(method);
			return;
		}
		if (method != PaymentMethod.CARD) {
			throw PaymentException.invalidRequest();
		}
	}

	@Getter
	@Builder
	public static class PaymentCreationResult {
		private UUID paymentId;
		private PaymentStatus paymentStatus;
		private OrderStatus orderStatus;
		private LocalDateTime approvedAt;
		private String orderNo;
		private Integer amount;
		private Long customerId;
	}

	/**
	 * 결제 명령 작업 결과 DTO (CQRS - Command Side)
	 */
	@Getter
	@Builder
	public static class PaymentDetailResult {
		private UUID paymentId;
		private UUID orderId;
		private PaymentMethod method;
		private PaymentStatus paymentStatus;
		private Integer amount;
		private LocalDateTime approvedAt;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
		private OrderStatus orderStatus;
	}

	private Payment loadPayment(UUID paymentId) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(PaymentException::paymentNotFound);
		if (payment.getDeletedAt() != null) {
			throw PaymentException.paymentNotFound();
		}
		return payment;
	}

	private void ensureStatus(Payment payment, PaymentStatus expected) {
		if (payment.getStatus() != expected) {
			throw OrderValidationException.invalidStatusTransition();
		}
	}

	private PaymentStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			throw PaymentException.invalidRequest();
		}
		try {
			return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw PaymentException.invalidRequest();
		}
	}

	private PaymentDetailResult approvePayment(Payment payment) {
		return approvePayment(payment, null, null);
	}

	private PaymentDetailResult approvePayment(Payment payment, String rawPayload, LocalDateTime approvedAt) {
		ensureStatus(payment, PaymentStatus.READY);

		// approvedAt이 null이면 현재 시간 사용 (기본 동작)
		LocalDateTime finalApprovedAt = approvedAt != null ? approvedAt : LocalDateTime.now();

		payment.setStatus(PaymentStatus.PAID);
		payment.setApprovedAt(finalApprovedAt);

		// rawPayload가 제공되면 업데이트
		if (rawPayload != null) {
			payment.setRawPayload(rawPayload);
		}

		Payment saved = paymentRepository.save(payment);
		OrderStatus orderStatus = updateOrderStatus(saved.getOrderId(), resolvePaymentOrderStatus(saved.getOrderId()), "결제 승인");

		// 결제 승인 이벤트 발행 (재고 차감, QR 생성 등을 비동기로 처리)
		Order order = orderRepository.findById(saved.getOrderId()).orElse(null);
		if (order != null) {
			OrderType orderType = resolveOrderType(saved.getOrderId());
			eventPublisher.publishEvent(new PaymentApprovedEvent(
					this,
					saved.getId(),
					saved.getOrderId(),
					saved.getMethod(),
					saved.getAmount(),
					orderType,
					order.getCustomerId(),
					finalApprovedAt
			));
		}

		return PaymentDetailResult.builder()
				.paymentId(saved.getId())
				.orderId(saved.getOrderId())
				.method(saved.getMethod())
				.paymentStatus(saved.getStatus())
				.amount(saved.getAmount())
				.approvedAt(saved.getApprovedAt())
				.createdAt(saved.getCreatedAt())
				.updatedAt(saved.getUpdatedAt())
				.orderStatus(orderStatus)
				.build();
	}

	private PaymentDetailResult failPayment(Payment payment) {
		ensureStatus(payment, PaymentStatus.READY);
		LocalDateTime failedAt = LocalDateTime.now();
		payment.setStatus(PaymentStatus.FAILED);
		Payment saved = paymentRepository.save(payment);

		// 결제 실패 이벤트 발행 (재고 복원 등을 비동기로 처리)
		Order order = orderRepository.findById(saved.getOrderId()).orElse(null);
		if (order != null) {
			OrderType orderType = resolveOrderType(saved.getOrderId());
			eventPublisher.publishEvent(new PaymentFailedEvent(
					this,
					saved.getId(),
					saved.getOrderId(),
					saved.getMethod(),
					saved.getAmount(),
					orderType,
					order.getCustomerId(),
					failedAt
			));
		}

		return PaymentDetailResult.builder()
				.paymentId(saved.getId())
				.orderId(saved.getOrderId())
				.method(saved.getMethod())
				.paymentStatus(saved.getStatus())
				.amount(saved.getAmount())
				.approvedAt(saved.getApprovedAt())
				.createdAt(saved.getCreatedAt())
				.updatedAt(saved.getUpdatedAt())
				.orderStatus(null)
				.build();
	}

	private PaymentDetailResult cancelPayment(Payment payment) {
		if (payment.getStatus() != PaymentStatus.READY && payment.getStatus() != PaymentStatus.PAID) {
			throw OrderValidationException.invalidStatusTransition();
		}

		// 결제 승인 후 5분이 지났는지 확인 (PAID 상태인 경우만)
		if (payment.getStatus() == PaymentStatus.PAID && payment.getApprovedAt() != null) {
			LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
			if (payment.getApprovedAt().isBefore(fiveMinutesAgo)) {
				throw PaymentException.cancellationTimeExpired();
			}
		}

		LocalDateTime cancelledAt = LocalDateTime.now();
		payment.setStatus(PaymentStatus.CANCELLED);
		Payment saved = paymentRepository.save(payment);
		OrderStatus orderStatus = updateOrderStatus(saved.getOrderId(), OrderStatus.CANCELLED, "결제 취소");

		// 결제 취소 이벤트 발행 (재고 복원 등을 비동기로 처리)
		Order order = orderRepository.findById(saved.getOrderId()).orElse(null);
		if (order != null) {
			OrderType orderType = resolveOrderType(saved.getOrderId());
			eventPublisher.publishEvent(new PaymentCancelledEvent(
					this,
					saved.getId(),
					saved.getOrderId(),
					saved.getMethod(),
					saved.getAmount(),
					orderType,
					order.getCustomerId(),
					cancelledAt
			));
		}

		return PaymentDetailResult.builder()
				.paymentId(saved.getId())
				.orderId(saved.getOrderId())
				.method(saved.getMethod())
				.paymentStatus(saved.getStatus())
				.amount(saved.getAmount())
				.approvedAt(saved.getApprovedAt())
				.createdAt(saved.getCreatedAt())
				.updatedAt(saved.getUpdatedAt())
				.orderStatus(orderStatus)
				.build();
	}

	private OrderStatus resolvePaymentOrderStatus(UUID orderId) {
		return OrderStatus.PAID;
	}

	private OrderStatus updateOrderStatus(UUID orderId, OrderStatus status, String reason) {
		Order updatedOrder = orderCommandService.updateStatus(orderId, status.name(), reason);
		return updatedOrder.getStatus();
	}

}
