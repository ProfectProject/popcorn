package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelRequest;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelResponse;
import com.popcorn.demo.domain.payment.event.PaymentSuccessEvent;
import com.popcorn.demo.common.cache.IdempotencyService;

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
	private final IdempotencyService idempotencyService;

	@Transactional(transactionManager = "jdbcTransactionManager", isolation = Isolation.READ_COMMITTED)
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

			// 🛡️ Step 0: PaymentKey 기반 중복 체크 - 동일한 결제 키로 이미 처리된 결제가 있는지 확인
		List<Payment> existingPaymentsByKey = paymentRepository.findByPaymentKeyInRawPayload(paymentKey);
		if (!existingPaymentsByKey.isEmpty()) {
			Payment existingPayment = existingPaymentsByKey.get(0);
			log.info("🛡️ 동일한 paymentKey로 이미 처리된 결제 발견 (중복 결제 차단): paymentKey={}, existingPaymentId={}",
					paymentKey, existingPayment.getId());

			// 기존 결제의 주문 정보 조회
			Order existingOrder = orderRepository.findById(existingPayment.getOrderId())
					.orElseThrow(OrderNotFoundException::orderNotFound);

			return TossPaymentConfirmResult.builder()
					.paymentId(existingPayment.getId())
					.paymentStatus(existingPayment.getStatus())
					.orderStatus(existingOrder.getStatus())
					.orderId(existingOrder.getId())
					.orderNo(existingOrder.getOrderNo())
					.amount(existingPayment.getAmount())
					.approvedAt(existingPayment.getApprovedAt())
					.build();
		}

		// 🛡️ Step 1: 멱등성 체크 - 이미 결제된 주문인지 확인
			List<Payment> existingPayments = paymentRepository
					.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(order.getId());

			if (!existingPayments.isEmpty()) {
				Payment existingPayment = existingPayments.get(0);

				// 이미 완료된 결제가 있는 경우
				if (existingPayment.getStatus() == PaymentStatus.PAID) {
					log.info("🛡️ 이미 결제 완료된 주문 (멱등성): orderId={}, paymentId={}", order.getId(), existingPayment.getId());
					OrderStatus orderStatus = order.getStatus();
					if (orderStatus != OrderStatus.PAID) {
						orderStatus = updateOrderStatus(order.getId(), OrderStatus.PAID, "결제 승인 (멱등성)");
					}
					return TossPaymentConfirmResult.builder()
							.paymentId(existingPayment.getId())
							.paymentStatus(existingPayment.getStatus())
							.orderStatus(orderStatus)
							.orderId(order.getId())
							.orderNo(order.getOrderNo())
							.amount(existingPayment.getAmount())
							.approvedAt(existingPayment.getApprovedAt())
							.build();
				}

				// 진행 중인 결제가 있는 경우 (READY 상태 등) - 중복 결제 차단
				if (existingPayment.getStatus() == PaymentStatus.READY) {
					log.warn("⚠️ 동일한 주문에 진행 중인 결제 발견 (중복 결제 시도 차단): orderId={}, existingPaymentId={}, status={}",
							order.getId(), existingPayment.getId(), existingPayment.getStatus());
					throw PaymentException.duplicatePaymentAttempt();
				}
			}

			// 🚀 Step 2: 토스 결제 승인 요청
			log.info("💳 토스 결제 승인 요청 시작: orderId={}, amount={}", order.getId(), amount);

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

			// 🎯 Step 3: 토스 승인 완료 후에만 결제 기록 생성
			LocalDateTime approvedAt = parseApprovedAt(response.getApprovedAt());
			String rawPayload = serializePayload(response);

			log.info("✅ 토스 승인 완료, 결제 기록 생성 시작: orderId={}", order.getId());

			PaymentCommandService.PaymentCreationResult paymentResult = paymentCommandService.createPayment(
					order.getId(),
					"CARD", // 기본값, 실제로는 response에서 추출 가능
					amount,
					rawPayload
			);

			// 생성된 결제 기록을 바로 PAID 상태로 업데이트
			Payment payment = paymentRepository.findById(paymentResult.getPaymentId())
					.orElseThrow(PaymentException::paymentNotFound);
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

			clearOrderIdempotencyCache(order);

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

	private void clearOrderIdempotencyCache(Order order) {
		Long customerId = order.getCustomerId();
		if (customerId == null) {
			return;
		}
		String prefix = "order_creation:" + customerId + ":create_order:";
		idempotencyService.clearByPrefix(prefix);
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

	/**
	 * 토스 결제 취소 처리
	 */
	@Transactional(transactionManager = "jdbcTransactionManager")
	public TossPaymentCancelResult cancelPayment(UUID orderId, String cancelReason) {
		log.info("🔄 토스 결제 취소 요청 - 주문ID: {}, 취소사유: {}", orderId, cancelReason);

		// 1. 주문 조회
		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderNotFoundException::orderNotFound);

		// 2. 결제 내역 조회
		List<Payment> payments = paymentRepository
				.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(order.getId());

		if (payments.isEmpty()) {
			throw PaymentException.paymentNotFound();
		}

		Payment payment = payments.get(0); // 가장 최근 결제
		if (payment.getStatus() != PaymentStatus.PAID) {
			throw PaymentException.invalidRequest();
		}

		// 3. 결제 원본 데이터에서 paymentKey 추출
		String paymentKey = extractPaymentKeyFromRawPayload(payment.getRawPayload());
		if (paymentKey == null) {
			throw PaymentException.invalidRequest();
		}

		// 4. 토스 결제 취소 요청
		TossPaymentsCancelResponse response = tossPaymentsClient.cancel(
				paymentKey,
				TossPaymentsCancelRequest.builder()
						.cancelReason(cancelReason)
						.build()
		);

		// 5. 결제 상태를 CANCELLED로 업데이트
		payment.setStatus(PaymentStatus.CANCELLED);
		payment.setRawPayload(serializePayload(response));
		Payment savedPayment = paymentRepository.save(payment);

		log.info("✅ 토스 결제 취소 완료 - 주문ID: {}, 결제ID: {}, 취소금액: {}원",
				orderId, savedPayment.getId(), response.getTotalAmount());

		return TossPaymentCancelResult.builder()
				.paymentId(savedPayment.getId())
				.orderId(orderId)
				.cancelAmount(response.getTotalAmount())
				.status(response.getStatus())
				.cancelReason(cancelReason)
				.build();
	}

	private String extractPaymentKeyFromRawPayload(String rawPayload) {
		if (rawPayload == null) {
			return null;
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			var node = mapper.readTree(rawPayload);
			return node.get("paymentKey").asText();
		} catch (Exception ex) {
			log.warn("결제 원본 데이터에서 paymentKey 추출 실패", ex);
			return null;
		}
	}

	private String serializePayload(TossPaymentsCancelResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException ex) {
			throw PaymentException.invalidRequest();
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

	@Getter
	@Builder
	public static class TossPaymentCancelResult {
		private UUID paymentId;
		private UUID orderId;
		private Integer cancelAmount;
		private String status;
		private String cancelReason;
	}
}
