package com.popcorn.demo.domain.qr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.popcorn.demo.domain.qr.checkin.repository.CheckinRepository;
import com.popcorn.demo.domain.qr.checkin.repository.CheckinRow;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;
import com.popcorn.demo.domain.qr.dto.response.QrVerifyResponse;
import com.popcorn.demo.domain.qr.exception.QrException;
import com.popcorn.demo.domain.qr.repository.QrCodeRepository;
import com.popcorn.demo.domain.qr.repository.QrCodeRow;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("QR 서비스 테스트")
class QrCodeServiceTest {

	private QrCodeRepository qrCodeRepository;
	private CheckinRepository checkinRepository;
	private JpaOrderItemRepository orderItemRepository;
	private ApplicationEventPublisher eventPublisher;
	private QrCodeService qrCodeService;

	@BeforeEach
	void setUp() {
		qrCodeRepository = Mockito.mock(QrCodeRepository.class);
		checkinRepository = Mockito.mock(CheckinRepository.class);
		orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
		eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		qrCodeService = new QrCodeService(qrCodeRepository, checkinRepository, orderItemRepository, eventPublisher);
	}

	@Test
	@DisplayName("PAID 예약 주문은 QR 신규 발급")
	void issue_createsQr_whenPaidReservation() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		LocalDateTime before = LocalDateTime.now();

		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("PAID"));
		when(qrCodeRepository.findLatestByOrderId(orderId)).thenReturn(Optional.empty());
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);

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

		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("PAID"));
		when(qrCodeRepository.findLatestByOrderId(orderId)).thenReturn(Optional.of(row));
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);

		QrCodeResponse response = qrCodeService.issue(orderId);

		assertThat(response.getQrCode()).isEqualTo("qr-exist-001");
		verify(qrCodeRepository, never()).insert(any(QrCodeRow.class));
	}

	@Test
	@DisplayName("PAID가 아니면 QR 발급 실패")
	void issue_throws_whenNotPaid() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");

		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("REQUESTED"));
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);

		assertThatThrownBy(() -> qrCodeService.issue(orderId))
				.isInstanceOf(QrException.class);
	}

	@Test
	@DisplayName("QR 검증 시 체크인 생성")
	void verify_createsCheckin() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001004");
		UUID qrId = UUID.fromString("90000000-0000-0000-0000-000000000001");
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000002");
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = new QrCodeRow(
				qrId,
				orderId,
				"qr-verify-001",
				now.plusMinutes(5),
				now.minusMinutes(1)
		);

		when(qrCodeRepository.findLatestByQrCode("qr-verify-001")).thenReturn(Optional.of(row));
		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("PAID"));
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
		when(checkinRepository.findLatestByOrderQrCodeId(qrId)).thenReturn(Optional.empty());
		when(checkinRepository.insert(eq(orderId), eq(qrId), isNull(), any(LocalDateTime.class)))
				.thenReturn(checkinId);

		QrVerifyResponse response = qrCodeService.verify("qr-verify-001");

		assertThat(response.isValid()).isTrue();
		assertThat(response.getCheckinId()).isEqualTo(checkinId);
	}

	@Test
	@DisplayName("QR 검증 시 기존 체크인 재사용")
	void verify_returnsExistingCheckin() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001005");
		UUID qrId = UUID.fromString("90000000-0000-0000-0000-000000000003");
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000004");
		LocalDateTime now = LocalDateTime.now();
		QrCodeRow row = new QrCodeRow(
				qrId,
				orderId,
				"qr-verify-002",
				now.plusMinutes(5),
				now.minusMinutes(1)
		);
		CheckinRow existing = new CheckinRow(
				checkinId,
				orderId,
				qrId,
				"qr-verify-002",
				now.minusMinutes(1),
				null
		);

		when(qrCodeRepository.findLatestByQrCode("qr-verify-002")).thenReturn(Optional.of(row));
		when(qrCodeRepository.findOrderStatus(orderId)).thenReturn(Optional.of("PAID"));
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
		when(checkinRepository.findLatestByOrderQrCodeId(qrId)).thenReturn(Optional.of(existing));

		QrVerifyResponse response = qrCodeService.verify("qr-verify-002");

		assertThat(response.getCheckinId()).isEqualTo(checkinId);
		verify(checkinRepository, never()).insert(any(), any(), any(), any());
	}
}
