package com.popcorn.checkIns.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QR 응답 코드 테스트")
class QrResponseCodeTest {

	@Test
	@DisplayName("QR_NOT_FOUND 응답 코드 확인")
	void qrNotFound_hasCorrectValues() {
		QrResponseCode code = QrResponseCode.QR_NOT_FOUND;

		assertThat(code.getCode()).isEqualTo(2000);
		assertThat(code.getHttpStatus()).isEqualTo(404);
		assertThat(code.getMessage()).isEqualTo("QR 코드를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("QR_EXPIRED 응답 코드 확인")
	void qrExpired_hasCorrectValues() {
		QrResponseCode code = QrResponseCode.QR_EXPIRED;

		assertThat(code.getCode()).isEqualTo(2001);
		assertThat(code.getHttpStatus()).isEqualTo(410);
		assertThat(code.getMessage()).isEqualTo("QR 코드가 만료되었습니다.");
	}

	@Test
	@DisplayName("ORDER_NOT_FOUND 응답 코드 확인")
	void orderNotFound_hasCorrectValues() {
		QrResponseCode code = QrResponseCode.ORDER_NOT_FOUND;

		assertThat(code.getCode()).isEqualTo(2002);
		assertThat(code.getHttpStatus()).isEqualTo(404);
		assertThat(code.getMessage()).isEqualTo("주문을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("ORDER_NOT_RESERVED 응답 코드 확인")
	void orderNotReserved_hasCorrectValues() {
		QrResponseCode code = QrResponseCode.ORDER_NOT_RESERVED;

		assertThat(code.getCode()).isEqualTo(2003);
		assertThat(code.getHttpStatus()).isEqualTo(409);
		assertThat(code.getMessage()).isEqualTo("결제 완료(PAID)된 예약 주문만 QR 발급이 가능합니다.");
	}

	@Test
	@DisplayName("모든 응답 코드가 코드 범위 내에 있음")
	void allCodes_areInValidRange() {
		for (QrResponseCode code : QrResponseCode.values()) {
			assertThat(code.getCode()).isBetween(2000, 2099);
		}
	}

	@Test
	@DisplayName("모든 응답 코드가 HTTP 상태 코드를 가짐")
	void allCodes_haveHttpStatus() {
		for (QrResponseCode code : QrResponseCode.values()) {
			assertThat(code.getHttpStatus()).isGreaterThan(0);
		}
	}

	@Test
	@DisplayName("모든 응답 코드가 메시지를 가짐")
	void allCodes_haveMessage() {
		for (QrResponseCode code : QrResponseCode.values()) {
			assertThat(code.getMessage()).isNotBlank();
		}
	}
}