package com.popcorn.checkIns.checkin.dto.owner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("오너 체크인 응답 코드 테스트")
class OwnerCheckinResponseCodeTest {

	@Test
	@DisplayName("UNAUTHENTICATED 응답 코드 확인")
	void unauthenticated_hasCorrectValues() {
		OwnerCheckinResponseCode code = OwnerCheckinResponseCode.UNAUTHENTICATED;

		assertThat(code.getCode()).isEqualTo(2601);
		assertThat(code.getHttpStatus()).isEqualTo(401);
		assertThat(code.getMessage()).isEqualTo("인증 정보가 없습니다.");
	}

	@Test
	@DisplayName("USER_ID_REQUIRED 응답 코드 확인")
	void userIdRequired_hasCorrectValues() {
		OwnerCheckinResponseCode code = OwnerCheckinResponseCode.USER_ID_REQUIRED;

		assertThat(code.getCode()).isEqualTo(2602);
		assertThat(code.getHttpStatus()).isEqualTo(400);
		assertThat(code.getMessage()).isEqualTo("사용자 ID가 필요합니다.");
	}

	@Test
	@DisplayName("USER_NOT_OWNER 응답 코드 확인")
	void userNotOwner_hasCorrectValues() {
		OwnerCheckinResponseCode code = OwnerCheckinResponseCode.USER_NOT_OWNER;

		assertThat(code.getCode()).isEqualTo(2605);
		assertThat(code.getHttpStatus()).isEqualTo(403);
		assertThat(code.getMessage()).isEqualTo("오너 권한이 필요합니다.");
	}

	@Test
	@DisplayName("SCHEDULE_NOT_FOUND 응답 코드 확인")
	void scheduleNotFound_hasCorrectValues() {
		OwnerCheckinResponseCode code = OwnerCheckinResponseCode.SCHEDULE_NOT_FOUND;

		assertThat(code.getCode()).isEqualTo(2608);
		assertThat(code.getHttpStatus()).isEqualTo(404);
		assertThat(code.getMessage()).isEqualTo("스케줄을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("모든 응답 코드가 코드 범위 내에 있음")
	void allCodes_areInValidRange() {
		for (OwnerCheckinResponseCode code : OwnerCheckinResponseCode.values()) {
			assertThat(code.getCode()).isBetween(2600, 2699);
		}
	}

	@Test
	@DisplayName("모든 응답 코드가 HTTP 상태 코드를 가짐")
	void allCodes_haveHttpStatus() {
		for (OwnerCheckinResponseCode code : OwnerCheckinResponseCode.values()) {
			assertThat(code.getHttpStatus()).isGreaterThan(0);
		}
	}

	@Test
	@DisplayName("모든 응답 코드가 메시지를 가짐")
	void allCodes_haveMessage() {
		for (OwnerCheckinResponseCode code : OwnerCheckinResponseCode.values()) {
			assertThat(code.getMessage()).isNotBlank();
		}
	}

	@Test
	@DisplayName("권한 관련 응답 코드 HTTP 상태 코드 확인")
	void authenticationRelatedCodes_haveCorrectHttpStatus() {
		assertThat(OwnerCheckinResponseCode.UNAUTHENTICATED.getHttpStatus()).isEqualTo(401);
		assertThat(OwnerCheckinResponseCode.USER_NOT_OWNER.getHttpStatus()).isEqualTo(403);
		assertThat(OwnerCheckinResponseCode.USER_ID_REQUIRED.getHttpStatus()).isEqualTo(400);
		assertThat(OwnerCheckinResponseCode.INVALID_PRINCIPAL.getHttpStatus()).isEqualTo(400);
	}
}