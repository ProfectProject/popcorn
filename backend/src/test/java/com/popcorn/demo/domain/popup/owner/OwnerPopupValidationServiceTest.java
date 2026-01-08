package com.popcorn.demo.domain.popup.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.service.owner.OwnerPopupValidationService;

class OwnerPopupValidationServiceTest {

	private final OwnerPopupValidationService validationService = new OwnerPopupValidationService();

	@Test
	@DisplayName("팝업 생성 요청 검증 성공")
	void 팝업_생성_요청_검증_성공() {
		CreatePopupRequest request = CreatePopupRequest.builder()
				.storeId(UUID.randomUUID())
				.title("테스트 팝업")
				.description("설명")
				.category(PopupCategory.FOOD)
				.schedules(List.of(CreatePopupScheduleRequest.builder()
						.startAt(LocalDateTime.now().plusDays(1))
						.endAt(LocalDateTime.now().plusDays(2))
						.price(10000)
						.capacity(10)
						.build()))
				.build();

		String title = validationService.validateCreateRequest(request);

		assertThat(title).isEqualTo("테스트 팝업");
	}

	@Test
	@DisplayName("팝업 생성 요청 검증 실패 - 스케줄 없음")
	void 팝업_생성_요청_검증_실패_스케줄_없음() {
		CreatePopupRequest request = CreatePopupRequest.builder()
				.storeId(UUID.randomUUID())
				.title("테스트 팝업")
				.category(PopupCategory.FOOD)
				.schedules(List.of())
				.build();

		assertThatThrownBy(() -> validationService.validateCreateRequest(request))
				.isInstanceOf(PopupException.class);
	}

	@Test
	@DisplayName("팝업 수정 요청 검증 실패 - 변경 사항 없음")
	void 팝업_수정_요청_검증_실패_변경_없음() {
		UpdatePopupRequest request = UpdatePopupRequest.builder().build();

		assertThatThrownBy(() -> validationService.validateUpdateRequest(request))
				.isInstanceOf(PopupException.class);
	}

	@Test
	@DisplayName("팝업 수정 요청 검증 성공 - 스케줄 수정")
	void 팝업_수정_요청_검증_성공_스케줄_수정() {
		UpdatePopupRequest request = UpdatePopupRequest.builder()
				.updateSchedules(List.of(UpdatePopupScheduleRequest.builder()
						.scheduleId(UUID.randomUUID())
						.startAt(LocalDateTime.now().plusDays(1))
						.endAt(LocalDateTime.now().plusDays(2))
						.price(2000)
						.capacity(5)
						.build()))
				.build();

		String title = validationService.validateUpdateRequest(request);

		assertThat(title).isNull();
	}
}
