package com.popcorn.checkIns.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("QR 검증 요청 DTO 테스트")
class QrVerifyRequestTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	@DisplayName("빌더 패턴으로 유효한 요청 생성")
	void builder_createsValidRequest() {
		// Given
		String qrCode = "valid-qr-code-123";

		// When
		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode(qrCode)
				.build();

		// Then
		assertThat(request.getQrCode()).isEqualTo(qrCode);

		Set<ConstraintViolation<QrVerifyRequest>> violations = validator.validate(request);
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("기본 생성자로 객체 생성")
	void noArgsConstructor_createsObject() {
		QrVerifyRequest request = new QrVerifyRequest();

		assertThat(request).isNotNull();
		assertThat(request.getQrCode()).isNull();
	}

	@Test
	@DisplayName("모든 인수 생성자로 객체 생성")
	void allArgsConstructor_createsObjectCorrectly() {
		String qrCode = "constructor-qr-code";

		QrVerifyRequest request = new QrVerifyRequest(qrCode);

		assertThat(request.getQrCode()).isEqualTo(qrCode);
	}

	@Test
	@DisplayName("QR 코드 null 값 검증 실패")
	void validation_failsWithNullQrCode() {
		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode(null)
				.build();

		Set<ConstraintViolation<QrVerifyRequest>> violations = validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("qrCode는 필수입니다.");
	}

	@Test
	@DisplayName("QR 코드 빈 문자열 검증 실패")
	void validation_failsWithEmptyQrCode() {
		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode("")
				.build();

		Set<ConstraintViolation<QrVerifyRequest>> violations = validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("qrCode는 필수입니다.");
	}

	@Test
	@DisplayName("QR 코드 공백 문자열 검증 실패")
	void validation_failsWithBlankQrCode() {
		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode("   ")
				.build();

		Set<ConstraintViolation<QrVerifyRequest>> violations = validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("qrCode는 필수입니다.");
	}

	@Test
	@DisplayName("유효한 QR 코드 검증 성공")
	void validation_succeedsWithValidQrCode() {
		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode("valid-qr-code-abc123")
				.build();

		Set<ConstraintViolation<QrVerifyRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("getter 메서드 작동 확인")
	void getter_returnsCorrectValue() {
		String expectedQrCode = "getter-test-qr";

		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode(expectedQrCode)
				.build();

		assertThat(request.getQrCode()).isEqualTo(expectedQrCode);
	}

	@Test
	@DisplayName("특수 문자를 포함한 QR 코드 유효성 검사")
	void validation_succeedsWithSpecialCharacters() {
		String qrCodeWithSpecialChars = "qr-code-!@#$%^&*()_+-={}[]|;':\",./<>?";

		QrVerifyRequest request = QrVerifyRequest.builder()
				.qrCode(qrCodeWithSpecialChars)
				.build();

		Set<ConstraintViolation<QrVerifyRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request.getQrCode()).isEqualTo(qrCodeWithSpecialChars);
	}
}