package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.qr.service.QrCodeService;
import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 조회 서비스 (CQRS - Query Side)
 *
 * 결제 관련 모든 조회 작업을 담당합니다.
 * Command Side와 분리하여 트랜잭션 충돌 문제를 해결합니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

	private static final Logger log = LoggerFactory.getLogger(PaymentQueryService.class);

	private final JpaPaymentRepository paymentRepository;
	private final JpaOrderItemRepository orderItemRepository;
	private final QrCodeService qrCodeService;

	/**
	 * 단건 결제 조회
	 */
	@Transactional(transactionManager = "jdbcTransactionManager", readOnly = true)
	public PaymentDetailResult getPayment(UUID paymentId) {
		Payment payment = findPaymentById(paymentId);
		return toDetailResult(payment, null);
	}

	/**
	 * 주문별 결제 목록 조회
	 */
	@Transactional(transactionManager = "jdbcTransactionManager", readOnly = true)
	public List<PaymentDetailResult> getPaymentsByOrder(UUID orderId) {
		return paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId)
				.stream()
				.map(payment -> toDetailResult(payment, null))
				.toList();
	}

	/**
	 * 결제 상세 정보 변환
	 */
	private PaymentDetailResult toDetailResult(Payment payment, OrderStatus orderStatus) {
		// QR 정보를 안전하게 조회 (트랜잭션 분리)
		QrInfo qrInfo = getQrInfoSafely(payment);

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
				.qrAvailable(qrInfo.available())
				.qrCode(qrInfo.code())
				.qrExpiresAt(qrInfo.expiresAt())
				.build();
	}

	/**
	 * QR 정보를 안전하게 조회
	 * 모든 예외를 포착하여 트랜잭션 안전성 보장
	 */
	private QrInfo getQrInfoSafely(Payment payment) {
		// 결제 완료 상태가 아니면 QR 없음
		if (payment.getStatus() != PaymentStatus.PAID) {
			return new QrInfo(false, null, null);
		}

		try {
			// 예약 주문인지 확인
			boolean isReservationOrder = orderItemRepository
					.existsByOrderIdAndSessionOptionIdIsNotNull(payment.getOrderId());

			if (!isReservationOrder) {
				return new QrInfo(false, null, null);
			}

			// QR 조회
			QrCodeResponse qrResponse = qrCodeService.get(payment.getOrderId());
			return new QrInfo(true, qrResponse.getQrCode(), qrResponse.getExpiresAt());

		} catch (com.popcorn.demo.domain.qr.exception.QrException e) {
			// QR 관련 정상적인 예외 (QR 없음, 만료 등)
			log.debug("QR 코드 없음 - 주문ID: {}, 사유: {}", payment.getOrderId(), e.getMessage());
			return new QrInfo(false, null, null);
		} catch (Exception e) {
			// 모든 예외를 안전하게 처리하여 트랜잭션 보호
			log.warn("QR 코드 조회 실패 - 주문ID: {}, 에러: {}", payment.getOrderId(), e.getMessage());
			return new QrInfo(false, null, null);
		}
	}

	/**
	 * 결제 엔티티 조회 (공통 메서드)
	 */
	private Payment findPaymentById(UUID paymentId) {
		try {
			Payment payment = paymentRepository.findById(paymentId)
					.orElseThrow(PaymentException::paymentNotFound);

			if (payment.getDeletedAt() != null) {
				throw PaymentException.paymentNotFound();
			}

			return payment;
		} catch (Exception e) {
			log.error("결제 조회 실패 - paymentId: {}", paymentId, e);
			throw PaymentException.paymentNotFound();
		}
	}

	/**
	 * 결제 상세 정보 DTO
	 */
	@Getter
	@Builder
	public static class PaymentDetailResult {
		private UUID paymentId;
		private UUID orderId;
		private com.popcorn.demo.domain.payment.entity.PaymentMethod method;
		private PaymentStatus paymentStatus;
		private Integer amount;
		private LocalDateTime approvedAt;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
		private OrderStatus orderStatus;
		// QR 코드 관련 정보
		private Boolean qrAvailable;
		private String qrCode;
		private LocalDateTime qrExpiresAt;
	}

	/**
	 * QR 정보 record
	 */
	private record QrInfo(Boolean available, String code, LocalDateTime expiresAt) {}
}