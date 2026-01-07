package com.popcorn.demo.domain.payment.service;

import com.popcorn.demo.domain.order.service.OrderCommandService;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

	private final OrderRepository orderRepository;
	private final OrderCommandService orderCommandService;
	private final JpaPaymentRepository paymentRepository;
	private final JpaOrderItemRepository orderItemRepository;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createReservationPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId);
		validateOrderType(orderId, true);
		PaymentMethod paymentMethod = parseMethod(method);
		validateReservationMethod(paymentMethod);
		return createPayment(order, paymentMethod, amount, rawPayload, PaymentStatus.READY, null);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createOrderPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId);
		validateOrderType(orderId, false);
		PaymentMethod paymentMethod = parseMethod(method);
		if (paymentMethod != PaymentMethod.CARD) {
			throw PaymentException.invalidRequest();
		}
		return createPayment(order, paymentMethod, amount, rawPayload, PaymentStatus.READY, null);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createReadyPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId);
		PaymentMethod paymentMethod = parseMethod(method);
		return createPayment(order, paymentMethod, amount, rawPayload, PaymentStatus.READY, null);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentDetailResult approvePayment(UUID paymentId) {
		Payment payment = loadPayment(paymentId);
		ensureStatus(payment, PaymentStatus.READY);
		PaymentStatus updatedStatus = PaymentStatus.PAID;
		LocalDateTime approvedAt = LocalDateTime.now();
		payment.setStatus(updatedStatus);
		payment.setApprovedAt(approvedAt);
		Payment saved = paymentRepository.save(payment);
		OrderStatus orderStatus = updateOrderStatus(saved.getOrderId(), resolvePaymentOrderStatus(saved.getOrderId()), "결제 승인");
		return toDetailResult(saved, orderStatus);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentDetailResult failPayment(UUID paymentId) {
		Payment payment = loadPayment(paymentId);
		ensureStatus(payment, PaymentStatus.READY);
		payment.setStatus(PaymentStatus.FAILED);
		Payment saved = paymentRepository.save(payment);
		return toDetailResult(saved, null);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentDetailResult cancelPayment(UUID paymentId) {
		Payment payment = loadPayment(paymentId);
		if (payment.getStatus() != PaymentStatus.READY && payment.getStatus() != PaymentStatus.PAID) {
			throw OrderValidationException.invalidStatusTransition();
		}
		payment.setStatus(PaymentStatus.CANCELLED);
		Payment saved = paymentRepository.save(payment);
		OrderStatus orderStatus = updateOrderStatus(saved.getOrderId(), OrderStatus.CANCELLED, "결제 취소");
		return toDetailResult(saved, orderStatus);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public void deletePayment(UUID paymentId) {
		Payment payment = loadPayment(paymentId);
		payment.setDeletedAt(LocalDateTime.now());
		paymentRepository.save(payment);
	}

	@Transactional(readOnly = true)
	public PaymentDetailResult getPayment(UUID paymentId) {
		Payment payment = loadPayment(paymentId);
		return toDetailResult(payment, null);
	}

	@Transactional(readOnly = true)
	public java.util.List<PaymentDetailResult> getPaymentsByOrder(UUID orderId) {
		return paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId)
				.stream()
				.map(payment -> toDetailResult(payment, null))
				.toList();
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

		return PaymentCreationResult.builder()
				.paymentId(saved.getId())
				.paymentStatus(paymentStatus)
				.orderStatus(order.getStatus())
				.approvedAt(approvedAt)
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

	private void validateOrderType(UUID orderId, boolean expectReservation) {
		boolean hasSchedule = orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId);
		boolean hasGoods = orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId);
		if (hasSchedule && hasGoods) {
			throw PaymentException.invalidRequest();
		}
		if (expectReservation && !hasSchedule) {
			throw PaymentException.invalidRequest();
		}
		if (!expectReservation && !hasGoods) {
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
	}

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

	private OrderStatus resolvePaymentOrderStatus(UUID orderId) {
		boolean hasSchedule = orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId);
		if (hasSchedule) {
			return OrderStatus.PAID;
		}
		return OrderStatus.COMPLETED;
	}

	private OrderStatus updateOrderStatus(UUID orderId, OrderStatus status, String reason) {
		Order updatedOrder = orderCommandService.updateStatus(orderId, status.name(), reason);
		return updatedOrder.getStatus();
	}

	private PaymentDetailResult toDetailResult(Payment payment, OrderStatus orderStatus) {
		return PaymentDetailResult.builder()
				.paymentId(payment.getId())
				.orderId(payment.getOrderId())
				.method(payment.getMethod())
				.paymentStatus(payment.getStatus())
				.amount(payment.getAmount())
				.approvedAt(payment.getApprovedAt())
				.createdAt(payment.getCreatedAt())
				.updatedAt(payment.getUpdatedAt())
				.orderStatus(orderStatus)
				.build();
	}
}
