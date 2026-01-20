package com.popcorn.checkIns.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QR 코드 응답 DTO 테스트")
class QrCodeResponseTest {

	@Test
	@DisplayName("빌더 패턴으로 객체 생성")
	void builder_createsObjectCorrectly() {
		// Given
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000001");
		String qrCode = "test-qr-code-123";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

		// When
		QrCodeResponse response = QrCodeResponse.builder()
				.orderId(orderId)
				.qrCode(qrCode)
				.expiresAt(expiresAt)
				.build();

		// Then
		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getQrCode()).isEqualTo(qrCode);
		assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("기본 생성자로 객체 생성")
	void noArgsConstructor_createsObject() {
		QrCodeResponse response = new QrCodeResponse();

		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isNull();
		assertThat(response.getQrCode()).isNull();
		assertThat(response.getExpiresAt()).isNull();
	}

	@Test
	@DisplayName("모든 인수 생성자로 객체 생성")
	void allArgsConstructor_createsObjectCorrectly() {
		// Given
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000002");
		String qrCode = "test-qr-code-456";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

		// When
		QrCodeResponse response = new QrCodeResponse(orderId, qrCode, expiresAt);

		// Then
		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getQrCode()).isEqualTo(qrCode);
		assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("null 값으로 객체 생성 가능")
	void builder_allowsNullValues() {
		QrCodeResponse response = QrCodeResponse.builder()
				.orderId(null)
				.qrCode(null)
				.expiresAt(null)
				.build();

		assertThat(response.getOrderId()).isNull();
		assertThat(response.getQrCode()).isNull();
		assertThat(response.getExpiresAt()).isNull();
	}

	@Test
	@DisplayName("getter 메서드 작동 확인")
	void getters_returnCorrectValues() {
		UUID orderId = UUID.randomUUID();
		String qrCode = "getter-test-qr";
		LocalDateTime expiresAt = LocalDateTime.now();

		QrCodeResponse response = QrCodeResponse.builder()
				.orderId(orderId)
				.qrCode(qrCode)
				.expiresAt(expiresAt)
				.build();

		assertThat(response.getOrderId()).isEqualTo(orderId);
		assertThat(response.getQrCode()).isEqualTo(qrCode);
		assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("빌더 패턴 부분적으로 사용 가능")
	void builder_partialBuild() {
		String qrCode = "partial-test-qr";

		QrCodeResponse response = QrCodeResponse.builder()
				.qrCode(qrCode)
				.build();

		assertThat(response.getOrderId()).isNull();
		assertThat(response.getQrCode()).isEqualTo(qrCode);
		assertThat(response.getExpiresAt()).isNull();
	}
}