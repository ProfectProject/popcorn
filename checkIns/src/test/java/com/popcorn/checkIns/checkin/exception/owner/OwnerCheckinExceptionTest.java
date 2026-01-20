package com.popcorn.checkIns.checkin.exception.owner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.checkIns.checkin.dto.owner.OwnerCheckinResponseCode;

@DisplayName("Owner 체크인 예외 테스트")
class OwnerCheckinExceptionTest {

	@Test
	@DisplayName("unauthenticated 예외 생성")
	void unauthenticated_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.unauthenticated();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.UNAUTHENTICATED);
		assertThat(exception.getMessage()).isNotNull();
		assertThat(exception).isInstanceOf(OwnerCheckinException.class);
	}

	@Test
	@DisplayName("userIdRequired 예외 생성")
	void userIdRequired_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.userIdRequired();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.USER_ID_REQUIRED);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("invalidPrincipal 예외 생성")
	void invalidPrincipal_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.invalidPrincipal();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.INVALID_PRINCIPAL);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("invalidRole 예외 생성")
	void invalidRole_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.invalidRole();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.INVALID_ROLE);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("notOwner 예외 생성")
	void notOwner_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.notOwner();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.USER_NOT_OWNER);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("popupIdRequired 예외 생성")
	void popupIdRequired_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.popupIdRequired();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.POPUP_ID_REQUIRED);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("scheduleIdRequired 예외 생성")
	void scheduleIdRequired_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.scheduleIdRequired();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.SCHEDULE_ID_REQUIRED);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("scheduleNotFound 예외 생성")
	void scheduleNotFound_createsException() {
		OwnerCheckinException exception = OwnerCheckinException.scheduleNotFound();

		assertThat(exception).isNotNull();
		assertThat(exception.getResponseCode()).isEqualTo(OwnerCheckinResponseCode.SCHEDULE_NOT_FOUND);
		assertThat(exception.getMessage()).isNotNull();
	}

	@Test
	@DisplayName("모든 정적 팩토리 메서드가 OwnerCheckinException을 반환")
	void allFactoryMethods_returnOwnerCheckinException() {
		assertThat(OwnerCheckinException.unauthenticated()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.userIdRequired()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.invalidPrincipal()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.invalidRole()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.notOwner()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.popupIdRequired()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.scheduleIdRequired()).isInstanceOf(OwnerCheckinException.class);
		assertThat(OwnerCheckinException.scheduleNotFound()).isInstanceOf(OwnerCheckinException.class);
	}

	@Test
	@DisplayName("예외 메시지가 존재함")
	void exceptionMessage_isPresent() {
		OwnerCheckinException exception = OwnerCheckinException.unauthenticated();

		assertThat(exception.getMessage()).isNotBlank();
	}
}