package com.popcorn.demo.domain.qr.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;
import com.popcorn.demo.domain.qr.dto.response.QrVerifyResponse;
import com.popcorn.demo.domain.qr.event.QrCheckinRequestedEvent;
import com.popcorn.demo.domain.qr.exception.QrException;
import com.popcorn.demo.domain.qr.repository.QrCodeRepository;
import com.popcorn.demo.domain.qr.repository.QrCodeRow;
import com.popcorn.demo.domain.qr.checkin.repository.CheckinRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QrCodeService {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

	private final QrCodeRepository qrCodeRepository;
	private final CheckinRepository checkinRepository;
	private final JpaOrderItemRepository orderItemRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public QrCodeResponse issue(UUID orderId) {
		String orderStatus = qrCodeRepository.findOrderStatus(orderId)
				.orElseThrow(QrException::orderNotFound);

		ensurePaid(orderStatus);
		ensureReservationOrder(orderId);

		LocalDateTime now = LocalDateTime.now();
		Optional<QrCodeRow> existing = qrCodeRepository.findLatestByOrderId(orderId)
				.filter(row -> !row.isExpired(now));

		if (existing.isPresent()) {
			return toResponse(existing.get());
		}

		QrCodeRow created = new QrCodeRow(
				UUID.randomUUID(),
				orderId,
				UUID.randomUUID().toString(),
				now.plus(DEFAULT_TTL),
				now
		);

		qrCodeRepository.insert(created);
		return toResponse(created);
	}

	@Transactional(transactionManager = "jdbcTransactionManager", readOnly = true)
	public QrCodeResponse get(UUID orderId) {
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = qrCodeRepository.findLatestByOrderId(orderId)
				.orElseThrow(QrException::qrNotFound);

		if (row.isExpired(now)) {
			throw QrException.qrExpired();
		}

		String orderStatus = qrCodeRepository.findOrderStatus(orderId)
				.orElseThrow(QrException::orderNotFound);

		ensurePaid(orderStatus);
		ensureReservationOrder(orderId);

		return toResponse(row);
	}

	@Transactional(transactionManager = "jdbcTransactionManager")
	public QrVerifyResponse verify(String qrCode) {
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = qrCodeRepository.findLatestByQrCode(qrCode)
				.orElseThrow(QrException::qrNotFound);

		if (row.isExpired(now)) {
			throw QrException.qrExpired();
		}

		String orderStatus = qrCodeRepository.findOrderStatus(row.orderId())
				.orElseThrow(QrException::orderNotFound);

		ensurePaid(orderStatus);
		ensureReservationOrder(row.orderId());

		java.util.Optional<com.popcorn.demo.domain.qr.checkin.repository.CheckinRow> existing =
				checkinRepository.findLatestByOrderQrCodeId(row.qrId());
		if (existing.isPresent()) {
			return QrVerifyResponse.builder()
					.valid(true)
					.checkinId(existing.get().checkinId())
					.orderId(row.orderId())
					.qrCode(row.qrCode())
					.expiresAt(row.expiresAt())
					.build();
		}

		// 동기적으로 체크인 처리 (응답 속도를 위해)
		java.util.UUID checkinId = checkinRepository.insert(
				row.orderId(),
				row.qrId(),
				null,
				now
		);

		// 체크인 요청 이벤트 발행 (비동기 후처리)
		eventPublisher.publishEvent(new QrCheckinRequestedEvent(
				this,
				row.qrId(),
				row.orderId(),
				row.qrCode(),
				row.expiresAt(),
				now
		));

		return QrVerifyResponse.builder()
				.valid(true)
				.checkinId(checkinId)
				.orderId(row.orderId())
				.qrCode(row.qrCode())
				.expiresAt(row.expiresAt())
				.build();
	}

	private static QrCodeResponse toResponse(QrCodeRow row) {
		return QrCodeResponse.builder()
				.orderId(row.orderId())
				.qrCode(row.qrCode())
				.expiresAt(row.expiresAt())
				.build();
	}

	private void ensurePaid(String orderStatus) {
		if (!"PAID".equals(orderStatus)) {
			throw QrException.orderNotReserved();
		}
	}

	private void ensureReservationOrder(UUID orderId) {
		boolean isReservation = orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId);
		if (!isReservation) {
			throw QrException.orderNotReserved();
		}
	}
}
