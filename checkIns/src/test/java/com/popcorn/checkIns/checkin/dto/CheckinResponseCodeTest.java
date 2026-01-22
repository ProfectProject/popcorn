package com.popcorn.checkIns.checkin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("체크인 응답 코드 테스트")
class CheckinResponseCodeTest {

	@Test
	@DisplayName("CHECKIN_NOT_FOUND 응답 코드 확인")
	void checkinNotFound_hasCorrectValues() {
		CheckinResponseCode code = CheckinResponseCode.CHECKIN_NOT_FOUND;

		assertThat(code.getCode()).isEqualTo(2100);
		assertThat(code.getHttpStatus()).isEqualTo(404);
		assertThat(code.getMessage()).isEqualTo("체크인을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("모든 응답 코드가 코드 범위 내에 있음")
	void allCodes_areInValidRange() {
		for (CheckinResponseCode code : CheckinResponseCode.values()) {
			assertThat(code.getCode()).isBetween(2100, 2199);
		}
	}

	@Test
	@DisplayName("모든 응답 코드가 HTTP 상태 코드를 가짐")
	void allCodes_haveHttpStatus() {
		for (CheckinResponseCode code : CheckinResponseCode.values()) {
			assertThat(code.getHttpStatus()).isGreaterThan(0);
		}
	}

	@Test
	@DisplayName("모든 응답 코드가 메시지를 가짐")
	void allCodes_haveMessage() {
		for (CheckinResponseCode code : CheckinResponseCode.values()) {
			assertThat(code.getMessage()).isNotBlank();
		}
	}
}