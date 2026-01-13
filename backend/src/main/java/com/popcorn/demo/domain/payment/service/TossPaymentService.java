package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.qr.service.QrCodeService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsClient;
import com.popcorn.demo.domain.payment.toss.TossPaymentsConfirmRequest;
import com.popcorn.demo.domain.payment.toss.TossPaymentsConfirmResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class TossPaymentService {

	private static final Logger log = LoggerFactory.getLogger(TossPaymentService.class);

	private final OrderRepository orderRepository;
	private final JpaPaymentRepository paymentRepository;
	private final OrderCommandService orderCommandService;
	private final JpaOrderItemRepository orderItemRepository;
	private final QrCodeService qrCodeService;
	private final TossPaymentsClient tossPaymentsClient;
	private final ObjectMapper objectMapper;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public TossPaymentConfirmResult confirmPayment(String paymentKey, String orderNo, Integer amount) {
		try {
			Order order = orderRepository.findByOrderNo(orderNo)
					.orElseThrow(OrderNotFoundException::orderNotFound);
			Payment payment = paymentRepository.findByOrderId(order.getId())
					.orElseThrow(PaymentException::paymentNotFound);
			if (payment.getDeletedAt() != null) {
				throw PaymentException.paymentNotFound();
			}
			ensureStatus(payment, PaymentStatus.READY);
			validateAmount(payment, amount);

			TossPaymentsConfirmResponse response = tossPaymentsClient.confirm(
					TossPaymentsConfirmRequest.builder()
							.paymentKey(paymentKey)
							.orderId(orderNo)
							.amount(amount)
							.build());
			if (response == null) {
				throw PaymentException.invalidRequest();
			}
			validateTotalAmount(response.getTotalAmount(), amount);

			LocalDateTime approvedAt = parseApprovedAt(response.getApprovedAt());
			payment.setStatus(PaymentStatus.PAID);
			payment.setApprovedAt(approvedAt);
			payment.setRawPayload(serializePayload(response));
			Payment saved = paymentRepository.save(payment);

			OrderStatus orderStatus = updateOrderStatus(saved.getOrderId(), OrderStatus.PAID, "결제 승인");
			issueReservationQr(saved.getOrderId());

			log.info("✅ 토스 결제 승인 완료 - orderNo: {}, orderId: {}, paymentId: {}, amount: {}, status: {}, orderStatus: {}",
					orderNo,
					order.getId(),
					saved.getId(),
					saved.getAmount(),
					saved.getStatus(),
					orderStatus);

			return TossPaymentConfirmResult.builder()
					.paymentId(saved.getId())
					.paymentStatus(saved.getStatus())
					.orderStatus(orderStatus)
					.orderId(order.getId())
					.orderNo(orderNo)
					.amount(saved.getAmount())
					.approvedAt(saved.getApprovedAt())
					.build();
		} catch (Exception ex) {
			log.error("❌ 토스 결제 승인 실패 - orderNo: {}, paymentKey: {}, amount: {}",
					orderNo,
					paymentKey,
					amount,
					ex);
			throw ex;
		}
	}

	private void validateAmount(Payment payment, Integer amount) {
		if (amount == null || amount <= 0) {
			throw PaymentException.invalidRequest();
		}
		if (!amount.equals(payment.getAmount())) {
			throw PaymentException.invalidRequest();
		}
	}

	private void validateTotalAmount(Integer totalAmount, Integer amount) {
		if (totalAmount != null && !totalAmount.equals(amount)) {
			throw PaymentException.invalidRequest();
		}
	}

	private void ensureStatus(Payment payment, PaymentStatus expected) {
		if (payment.getStatus() != expected) {
			throw OrderValidationException.invalidStatusTransition();
		}
	}

	private OrderStatus updateOrderStatus(UUID orderId, OrderStatus status, String reason) {
		return orderCommandService.updateStatus(orderId, status.name(), reason).getStatus();
	}

	private void issueReservationQr(UUID orderId) {
		boolean isReservation = orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId);
		if (isReservation) {
			qrCodeService.issue(orderId);
		}
	}

	private String serializePayload(TossPaymentsConfirmResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException ex) {
			throw PaymentException.invalidRequest();
		}
	}

	private LocalDateTime parseApprovedAt(String approvedAt) {
		if (approvedAt == null || approvedAt.isBlank()) {
			return LocalDateTime.now();
		}
		try {
			return OffsetDateTime.parse(approvedAt).toLocalDateTime();
		} catch (Exception ex) {
			return LocalDateTime.now();
		}
	}

	@Getter
	@Builder
	public static class TossPaymentConfirmResult {
		private UUID paymentId;
		private PaymentStatus paymentStatus;
		private OrderStatus orderStatus;
		private UUID orderId;
		private String orderNo;
		private Integer amount;
		private LocalDateTime approvedAt;
	}
}
