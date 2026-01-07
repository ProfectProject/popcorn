package com.popcorn.demo.domain.qr.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;
import com.popcorn.demo.domain.qr.dto.response.QrVerifyResponse;
import com.popcorn.demo.domain.qr.exception.QrException;
import com.popcorn.demo.domain.qr.repository.QrCodeRepository;
import com.popcorn.demo.domain.qr.repository.QrCodeRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QrCodeService {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

	private final QrCodeRepository qrCodeRepository;

	@Transactional
	public QrCodeResponse issue(UUID orderId) {
		String orderStatus = qrCodeRepository.findOrderStatus(orderId)
				.orElseThrow(QrException::orderNotFound);

		if (!"RESERVED".equals(orderStatus)) {
			throw QrException.orderNotReserved();
		}

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

	@Transactional(readOnly = true)
	public QrCodeResponse get(UUID orderId) {
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = qrCodeRepository.findLatestByOrderId(orderId)
				.orElseThrow(QrException::qrNotFound);

		if (row.isExpired(now)) {
			throw QrException.qrExpired();
		}

		String orderStatus = qrCodeRepository.findOrderStatus(orderId)
				.orElseThrow(QrException::orderNotFound);

		if (!"RESERVED".equals(orderStatus)) {
			throw QrException.orderNotReserved();
		}

		return toResponse(row);
	}

	@Transactional(readOnly = true)
	public QrVerifyResponse verify(String qrCode) {
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = qrCodeRepository.findLatestByQrCode(qrCode)
				.orElseThrow(QrException::qrNotFound);

		if (row.isExpired(now)) {
			throw QrException.qrExpired();
		}

		String orderStatus = qrCodeRepository.findOrderStatus(row.orderId())
				.orElseThrow(QrException::orderNotFound);

		if (!"RESERVED".equals(orderStatus)) {
			throw QrException.orderNotReserved();
		}

		return QrVerifyResponse.builder()
				.valid(true)
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
}
