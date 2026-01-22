package com.popcorn.checkIns.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.checkIns.dto.QrResponseCode;

@DisplayName("QR 예외 테스트")
class QrExceptionTest {

	@Test
	@DisplayName("QR 코드를 찾을 수 없음 예외 생성")
	void qrNotFound_createsException() {
		QrException exception = QrException.qrNotFound();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(QrResponseCode.QR_NOT_FOUND);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("QR 코드 만료됨 예외 생성")
	void qrExpired_createsException() {
		QrException exception = QrException.qrExpired();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(QrResponseCode.QR_EXPIRED);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("주문을 찾을 수 없음 예외 생성")
	void orderNotFound_createsException() {
		QrException exception = QrException.orderNotFound();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(QrResponseCode.ORDER_NOT_FOUND);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("예약되지 않은 주문 예외 생성")
	void orderNotReserved_createsException() {
		QrException exception = QrException.orderNotReserved();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(QrResponseCode.ORDER_NOT_RESERVED);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("모든 정적 팩토리 메서드가 QrException을 반환")
	void allFactoryMethods_returnQrException() {
		assertThat(QrException.qrNotFound()).isInstanceOf(QrException.class);
		assertThat(QrException.qrExpired()).isInstanceOf(QrException.class);
		assertThat(QrException.orderNotFound()).isInstanceOf(QrException.class);
		assertThat(QrException.orderNotReserved()).isInstanceOf(QrException.class);
	}

	@Test
	@DisplayName("예외 메시지가 존재함")
	void exceptionMessage_isPresent() {
		QrException exception = QrException.qrNotFound();

		assertThat(exception.getMessage()).isNotBlank();
	}
}