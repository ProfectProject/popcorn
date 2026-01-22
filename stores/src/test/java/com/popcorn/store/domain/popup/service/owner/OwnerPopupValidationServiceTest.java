package com.popcorn.store.domain.popup.service.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.store.domain.popup.dto.owner.OwnerPopupResponseCode;
import com.popcorn.store.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.store.domain.popup.dto.owner.request.CreatePopupScheduleRequest;
import com.popcorn.store.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.store.domain.popup.dto.owner.request.UpdatePopupScheduleRequest;
import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.exception.owner.OwnerPopupException;

class OwnerPopupValidationServiceTest {

	private final OwnerPopupValidationService service = new OwnerPopupValidationService();

	@Test
	@DisplayName("팝업 제목 검증 - 빈 값이면 실패")
	void validateTitleRejectsBlank() {
		assertThatThrownBy(() -> service.validateAndTrimTitle("  "))
				.isInstanceOf(OwnerPopupException.class)
				.extracting("responseCode")
				.isEqualTo(OwnerPopupResponseCode.TITLE_REQUIRED);
	}

	@Test
	@DisplayName("팝업 제목 검증 - 금지 문자 포함 시 실패")
	void validateTitleRejectsInvalidChars() {
		assertThatThrownBy(() -> service.validateAndTrimTitle("bad<title>"))
				.isInstanceOf(OwnerPopupException.class)
				.extracting("responseCode")
				.isEqualTo(OwnerPopupResponseCode.TITLE_INVALID_CHARS);
	}

	@Test
	@DisplayName("팝업 생성 요청 - 스케줄 없으면 실패")
	void validateCreateRequestRequiresSchedules() {
		CreatePopupRequest request = CreatePopupRequest.builder()
				.storeId(UUID.randomUUID())
				.title("팝업")
				.category(PopupCategory.FOOD)
				.schedules(List.of())
				.build();

		assertThatThrownBy(() -> service.validateCreateRequest(request))
				.isInstanceOf(OwnerPopupException.class)
				.extracting("responseCode")
				.isEqualTo(OwnerPopupResponseCode.SCHEDULES_REQUIRED);
	}

	@Test
	@DisplayName("팝업 생성 요청 - 정상 요청이면 제목 반환")
	void validateCreateRequestReturnsTitle() {
		CreatePopupScheduleRequest schedule = CreatePopupScheduleRequest.builder()
				.startAt(LocalDateTime.of(2025, 1, 1, 10, 0))
				.endAt(LocalDateTime.of(2025, 1, 1, 12, 0))
				.price(10000)
				.capacity(10)
				.build();
		CreatePopupRequest request = CreatePopupRequest.builder()
				.storeId(UUID.randomUUID())
				.title("  테스트 팝업  ")
				.category(PopupCategory.FOOD)
				.schedules(List.of(schedule))
				.build();

		String trimmed = service.validateCreateRequest(request);

		assertThat(trimmed).isEqualTo("테스트 팝업");
	}

	@Test
	@DisplayName("팝업 수정 요청 - 변경 사항이 없으면 실패")
	void validateUpdateRequestRequiresChanges() {
		UpdatePopupRequest request = UpdatePopupRequest.builder().build();

		assertThatThrownBy(() -> service.validateUpdateRequest(request))
				.isInstanceOf(OwnerPopupException.class)
				.extracting("responseCode")
				.isEqualTo(OwnerPopupResponseCode.UPDATE_NO_CHANGES);
	}

	@Test
	@DisplayName("팝업 수정 요청 - 스케줄 시간 쌍 누락이면 실패")
	void validateUpdateRequestRejectsScheduleTimePair() {
		UpdatePopupScheduleRequest updateSchedule = UpdatePopupScheduleRequest.builder()
				.scheduleId(UUID.randomUUID())
				.startAt(LocalDateTime.of(2025, 1, 1, 10, 0))
				.build();
		UpdatePopupRequest request = UpdatePopupRequest.builder()
				.updateSchedules(List.of(updateSchedule))
				.build();

		assertThatThrownBy(() -> service.validateUpdateRequest(request))
				.isInstanceOf(OwnerPopupException.class)
				.extracting("responseCode")
				.isEqualTo(OwnerPopupResponseCode.SCHEDULE_TIME_PAIR_REQUIRED);
	}
}
