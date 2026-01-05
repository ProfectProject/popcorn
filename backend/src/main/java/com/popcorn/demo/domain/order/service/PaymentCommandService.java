package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.Payment;
import com.popcorn.demo.domain.order.entity.PaymentMethod;
import com.popcorn.demo.domain.order.entity.PaymentStatus;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.exception.PaymentException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaPaymentRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;

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
		return createPayment(order, paymentMethod, amount, rawPayload, OrderStatus.PAID);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public PaymentCreationResult createOrderPayment(UUID orderId, String method, Integer amount, String rawPayload) {
		Order order = loadOrder(orderId);
		validateOrderType(orderId, false);
		PaymentMethod paymentMethod = parseMethod(method);
		if (paymentMethod != PaymentMethod.CARD) {
			throw PaymentException.invalidRequest();
		}
		return createPayment(order, paymentMethod, amount, rawPayload, OrderStatus.COMPLETED);
	}

	private Order loadOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderNotFoundException::orderNotFound);
		if (order.getStatus() == OrderStatus.CANCELLED) {
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

		PaymentStatus paymentStatus = PaymentStatus.PAID;
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
				&& method != PaymentMethod.TRANSFER
				&& method != PaymentMethod.EASY_PAY) {
			throw PaymentException.invalidRequest();
		}
	}

	private void validateOrderType(UUID orderId, boolean expectReservation) {
		boolean hasSchedule = orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId);
		boolean hasGoods = orderItemRepository.existsByOrderIdAndMerchVariantIdIsNotNull(orderId);
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
}
