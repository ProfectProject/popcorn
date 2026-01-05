package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.entity.Payment;
import com.popcorn.demo.domain.order.entity.PaymentMethod;
import com.popcorn.demo.domain.order.entity.PaymentStatus;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.exception.PaymentException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaPaymentRepository;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

	private final OrderRepository orderRepository;
	private final OrderCommandService orderCommandService;
	private final JpaPaymentRepository paymentRepository;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createReservationPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId, OrderType.RESERVATION);
		PaymentMethod paymentMethod = parseMethod(method);
		validateReservationMethod(paymentMethod);
		return createPayment(order, paymentMethod, amount, rawPayload, OrderStatus.PAID);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createOrderPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId, OrderType.PURCHASE);
		PaymentMethod paymentMethod = parseMethod(method);
		if (paymentMethod != PaymentMethod.CARD) {
			throw PaymentException.invalidRequest();
		}
		return createPayment(order, paymentMethod, amount, rawPayload, OrderStatus.COMPLETED);
	}

	private Order loadOrder(UUID orderId, OrderType expectedType) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderNotFoundException::orderNotFound);
		if (order.getOrderType() != expectedType) {
			throw PaymentException.invalidRequest();
		}
		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
			throw OrderValidationException.invalidStatusTransition();
		}
		if (paymentRepository.existsByOrderId(orderId)) {
			throw PaymentException.paymentAlreadyExists();
		}
		return order;
	}

	private PaymentCreationResult createPayment(
			Order order,
			PaymentMethod method,
			Integer amount,
			String rawPayload,
			OrderStatus successStatus) {
		if (amount == null || amount <= 0) {
			throw PaymentException.invalidRequest();
		}

		PaymentStatus paymentStatus = PaymentStatus.APPROVED;
		LocalDateTime approvedAt = LocalDateTime.now();

		Payment payment = Payment.builder()
				.orderId(order.getId())
				.method(method)
				.status(paymentStatus)
				.amount(amount)
				.rawPayload(rawPayload)
				.approvedAt(approvedAt)
				.build();

		Payment saved = paymentRepository.save(payment);

		OrderStatus updatedStatus = resolveUpdatedStatus(order, successStatus);

		return PaymentCreationResult.builder()
				.paymentId(saved.getId())
				.paymentStatus(paymentStatus)
				.orderStatus(updatedStatus)
				.approvedAt(approvedAt)
				.build();
	}

	private OrderStatus resolveUpdatedStatus(Order order, OrderStatus successStatus) {
		if (successStatus == null) {
			return order.getStatus();
		}
		if (order.getStatus() == successStatus) {
			return successStatus;
		}
		Order updatedOrder = orderCommandService.updateStatus(
				order.getId(),
				successStatus.name(),
				"결제 완료");
		return updatedOrder.getStatus();
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
				&& method != PaymentMethod.CASH
				&& method != PaymentMethod.TRANSFER) {
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
}
