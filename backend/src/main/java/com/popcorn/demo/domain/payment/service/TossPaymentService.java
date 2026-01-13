package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
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
import com.popcorn.demo.domain.payment.event.PaymentSuccessEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
public class TossPaymentService {

	private static final Logger log = LoggerFactory.getLogger(TossPaymentService.class);

	private final OrderRepository orderRepository;
	private final JpaPaymentRepository paymentRepository;
	private final OrderCommandService orderCommandService;
	private final PaymentCommandService paymentCommandService;
	private final JpaOrderItemRepository orderItemRepository;
	private final TossPaymentsClient tossPaymentsClient;
	private final ObjectMapper objectMapper;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public TossPaymentConfirmResult confirmPayment(String paymentKey, String orderId, Integer amount) {
		try {
			Order order;
			String tossOrderId = orderId;
			try {
				UUID orderUuid = UUID.fromString(orderId);
				order = orderRepository.findById(orderUuid)
						.orElseThrow(OrderNotFoundException::orderNotFound);
			} catch (IllegalArgumentException ex) {
				// Backward compatibility: allow orderNo as orderId.
				order = orderRepository.findByOrderNo(orderId)
						.orElseThrow(OrderNotFoundException::orderNotFound);
				tossOrderId = order.getOrderNo();
			}
			List<Payment> payments = paymentRepository
					.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(order.getId());
			if (payments.isEmpty()) {
				throw PaymentException.paymentNotFound();
			}
			Payment payment = payments.get(0);
			if (payment.getDeletedAt() != null) {
				throw PaymentException.paymentNotFound();
			}
			if (payment.getStatus() == PaymentStatus.PAID) {
				OrderStatus orderStatus = order.getStatus();
				if (orderStatus != OrderStatus.PAID) {
					orderStatus = updateOrderStatus(order.getId(), OrderStatus.PAID, "결제 승인");
				}
				return TossPaymentConfirmResult.builder()
						.paymentId(payment.getId())
						.paymentStatus(payment.getStatus())
						.orderStatus(orderStatus)
						.orderId(order.getId())
						.orderNo(order.getOrderNo())
						.amount(payment.getAmount())
						.approvedAt(payment.getApprovedAt())
						.build();
			}
			ensureStatus(payment, PaymentStatus.READY);
			validateAmount(payment, amount);

			TossPaymentsConfirmResponse response = tossPaymentsClient.confirm(
					TossPaymentsConfirmRequest.builder()
							.paymentKey(paymentKey)
							.orderId(tossOrderId)
							.amount(amount)
							.build());
			if (response == null) {
				throw PaymentException.invalidRequest();
			}
			validateTotalAmount(response.getTotalAmount(), amount);

			// 결제 상태 직접 업데이트 (rawPayload와 approvedAt 포함)
			LocalDateTime approvedAt = parseApprovedAt(response.getApprovedAt());
			String rawPayload = serializePayload(response);

			payment.setStatus(PaymentStatus.PAID);
			payment.setApprovedAt(approvedAt);
			payment.setRawPayload(rawPayload);
			Payment saved = paymentRepository.save(payment);

			OrderStatus orderStatus = updateOrderStatus(saved.getOrderId(), OrderStatus.PAID, "결제 승인");

			// 결제 성공 이벤트 발행 (비동기 후속 작업 트리거)
			publishPaymentSuccessEvent(order, saved, approvedAt, paymentKey);

			log.info("✅ 토스 결제 승인 완료 - orderNo: {}, orderId: {}, paymentId: {}, amount: {}, status: {}, orderStatus: {}, approvedAt: {}",
					order.getOrderNo(),
					order.getId(),
					saved.getId(),
					saved.getAmount(),
					saved.getStatus(),
					orderStatus,
					approvedAt);

			log.info("🎯 결제 데이터 저장 완료 - rawPayload 길이: {} bytes, approvedAt: {}",
					rawPayload.length(),
					approvedAt);

			return TossPaymentConfirmResult.builder()
					.paymentId(saved.getId())
					.paymentStatus(saved.getStatus())
					.orderStatus(orderStatus)
					.orderId(order.getId())
					.orderNo(order.getOrderNo())
					.amount(saved.getAmount())
					.approvedAt(saved.getApprovedAt())
					.build();
		} catch (Exception ex) {
			log.error("❌ 토스 결제 승인 실패 - orderId: {}, paymentKey: {}, amount: {}",
					orderId,
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

	/**
	 * 결제 성공 이벤트 발행 (비동기 후속 작업 트리거)
	 */
	private void publishPaymentSuccessEvent(Order order, Payment payment, LocalDateTime approvedAt, String paymentKey) {
		try {
			// 주문 항목 조회
			List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

			// 이벤트 생성 및 발행
			PaymentSuccessEvent event = PaymentSuccessEvent.builder()
					.orderId(order.getId())
					.orderNo(order.getOrderNo())
					.paymentId(payment.getId())
					.orderType(order.getOrderType() != null ? order.getOrderType().name() : "PURCHASE") // RESERVATION or PURCHASE
					.totalAmount(payment.getAmount())
					.userId(order.getCustomerId())
					.orderItems(orderItems)
					.paidAt(approvedAt)
					.paymentMethod("TOSS")
					.paymentKey(paymentKey)
					.build();

			eventPublisher.publishEvent(event);
			log.info("📨 결제 성공 이벤트 발행 완료 - orderNo: {}, paymentId: {}",
					order.getOrderNo(), payment.getId());

		} catch (Exception e) {
			// 이벤트 발행 실패해도 결제는 성공으로 처리
			log.error("❌ 결제 성공 이벤트 발행 실패 - orderNo: {}, error: {}",
					order.getOrderNo(), e.getMessage(), e);
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
