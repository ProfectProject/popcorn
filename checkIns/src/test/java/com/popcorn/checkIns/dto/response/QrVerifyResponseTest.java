package com.popcorn.checkIns.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QR 검증 응답 DTO 테스트")
class QrVerifyResponseTest {

	@Test
	@DisplayName("빌더 패턴으로 유효한 검증 응답 생성")
	void builder_createsValidResponse() {
		// Given
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000001");
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000001");
		String qrCode = "valid-qr-code";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

		// When
		QrVerifyResponse response = QrVerifyResponse.builder()
				.valid(true)
				.checkinId(checkinId)
				.orderId(orderId)
				.qrCode(qrCode)
				.expiresAt(expiresAt)
				.build();

		// Then
		assertThat(response.isValid()).isTrue();
		assertThat(response.getCheckinId()).isEqualTo(checkinId);
		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getQrCode()).isEqualTo(qrCode);
		assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("빌더 패턴으로 무효한 검증 응답 생성")
	void builder_createsInvalidResponse() {
		// When
		QrVerifyResponse response = QrVerifyResponse.builder()
				.valid(false)
				.build();

		// Then
		assertThat(response.isValid()).isFalse();
		assertThat(response.getCheckinId()).isNull();
		assertThat(response.getOrderId()).isNull();
		assertThat(response.getQrCode()).isNull();
		assertThat(response.getExpiresAt()).isNull();
	}

	@Test
	@DisplayName("기본 생성자로 객체 생성")
	void noArgsConstructor_createsObject() {
		QrVerifyResponse response = new QrVerifyResponse();

		assertThat(response).isNotNull();
		assertThat(response.isValid()).isFalse(); // primitive boolean 기본값
		assertThat(response.getCheckinId()).isNull();
		assertThat(response.getOrderId()).isNull();
		assertThat(response.getQrCode()).isNull();
		assertThat(response.getExpiresAt()).isNull();
	}

	@Test
	@DisplayName("모든 인수 생성자로 객체 생성")
	void allArgsConstructor_createsObjectCorrectly() {
		// Given
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000002");
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000002");
		String qrCode = "constructor-qr-code";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

		// When
		QrVerifyResponse response = new QrVerifyResponse(true, checkinId, orderId, qrCode, expiresAt);

		// Then
		assertThat(response.isValid()).isTrue();
		assertThat(response.getCheckinId()).isEqualTo(checkinId);
		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getQrCode()).isEqualTo(qrCode);
		assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("valid 필드만 설정한 경우")
	void builder_onlyValidField() {
		QrVerifyResponse validResponse = QrVerifyResponse.builder()
				.valid(true)
				.build();

		QrVerifyResponse invalidResponse = QrVerifyResponse.builder()
				.valid(false)
				.build();

		assertThat(validResponse.isValid()).isTrue();
		assertThat(invalidResponse.isValid()).isFalse();
	}

	@Test
	@DisplayName("체크인 ID만 있는 부분적인 응답")
	void builder_partialResponseWithCheckinId() {
		UUID checkinId = UUID.randomUUID();

		QrVerifyResponse response = QrVerifyResponse.builder()
				.valid(true)
				.checkinId(checkinId)
				.build();

		assertThat(response.isValid()).isTrue();
		assertThat(response.getCheckinId()).isEqualTo(checkinId);
		assertThat(response.getOrderId()).isNull();
		assertThat(response.getQrCode()).isNull();
		assertThat(response.getExpiresAt()).isNull();
	}

	@Test
	@DisplayName("만료 시간 설정 확인")
	void verifyResponse_hasExpirationTime() {
		LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

		QrVerifyResponse response = QrVerifyResponse.builder()
				.valid(true)
				.expiresAt(expiresAt)
				.build();

		assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(response.isValid()).isTrue();
	}
}