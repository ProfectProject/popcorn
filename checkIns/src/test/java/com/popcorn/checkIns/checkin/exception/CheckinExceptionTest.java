package com.popcorn.checkIns.checkin.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.checkIns.checkin.dto.CheckinResponseCode;

@DisplayName("체크인 예외 테스트")
class CheckinExceptionTest {

	@Test
	@DisplayName("체크인을 찾을 수 없음 예외 생성")
	void notFound_createsException() {
		CheckinException exception = CheckinException.notFound();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(CheckinResponseCode.CHECKIN_NOT_FOUND);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("예외가 CheckinException 인스턴스임")
	void notFound_returnsCheckinException() {
		CheckinException exception = CheckinException.notFound();

		assertThat(exception).isInstanceOf(CheckinException.class);
	}

	@Test
	@DisplayName("예외 메시지가 존재함")
	void exceptionMessage_isPresent() {
		CheckinException exception = CheckinException.notFound();

		assertThat(exception.getMessage()).isNotBlank();
	}

	@Test
	@DisplayName("정적 팩토리 메서드가 올바른 응답 코드 설정")
	void factoryMethod_setsCorrectResponseCode() {
		CheckinException exception = CheckinException.notFound();

		assertThat(exception.getResponseCode()).isEqualTo(CheckinResponseCode.CHECKIN_NOT_FOUND);
	}
}