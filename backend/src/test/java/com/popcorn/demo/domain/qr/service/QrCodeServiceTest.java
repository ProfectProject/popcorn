package com.popcorn.demo.domain.qr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.checkin.repository.CheckinRepository;
import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;
import com.popcorn.demo.domain.qr.exception.QrException;
import com.popcorn.demo.domain.qr.repository.QrCodeRepository;
import com.popcorn.demo.domain.qr.repository.QrCodeRow;

@DisplayName("QR 서비스 테스트")
class QrCodeServiceTest {

	private QrCodeRepository qrCodeRepository;
	private CheckinRepository checkinRepository;
	private QrCodeService qrCodeService;

	@BeforeEach
	void setUp() {
		qrCodeRepository = Mockito.mock(QrCodeRepository.class);
		checkinRepository = Mockito.mock(CheckinRepository.class);
		qrCodeService = new QrCodeService(qrCodeRepository, checkinRepository);
	}

	@Test
	@DisplayName("RESERVED 주문은 QR 신규 발급")
	void issue_createsQr_whenReserved() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		LocalDateTime before = LocalDateTime.now();

		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("RESERVED"));
		when(qrCodeRepository.findLatestByOrderId(orderId)).thenReturn(Optional.empty());

		QrCodeResponse response = qrCodeService.issue(orderId);

		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getQrCode()).isNotBlank();
		assertThat(response.getExpiresAt()).isAfter(before);
		verify(qrCodeRepository).insert(any(QrCodeRow.class));
	}

	@Test
	@DisplayName("만료되지 않은 QR이 있으면 재발급하지 않음")
	void issue_returnsExisting_whenNotExpired() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001002");
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = new QrCodeRow(
				UUID.randomUUID(),
				orderId,
				"qr-exist-001",
				now.plusMinutes(5),
				now.minusMinutes(1)
		);

		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("RESERVED"));
		when(qrCodeRepository.findLatestByOrderId(orderId)).thenReturn(Optional.of(row));

		QrCodeResponse response = qrCodeService.issue(orderId);

		assertThat(response.getQrCode()).isEqualTo("qr-exist-001");
		verify(qrCodeRepository, never()).insert(any(QrCodeRow.class));
	}

	@Test
	@DisplayName("RESERVED가 아니면 QR 발급 실패")
	void issue_throws_whenNotReserved() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");

		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("REQUESTED"));

		assertThatThrownBy(() -> qrCodeService.issue(orderId))
				.isInstanceOf(QrException.class);
	}
}
