package com.popcorn.demo.domain.popup.owner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.popup.dto.owner.OwnerPopupResponseCode;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupCreatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDeletedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDetailDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupListDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupScheduleCreatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupStatusUpdatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupUpdatedDto;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.exception.owner.OwnerPopupException;

class OwnerPopupDtoTest {

	@Test
	@DisplayName("오너 팝업 응답 DTO 빌더가 정상 동작한다")
	void ownerPopupDtoBuilders() {
		UUID popupId = UUID.randomUUID();

		PopupCreatedDto created = PopupCreatedDto.builder()
				.popupId(popupId)
				.storeId(UUID.randomUUID())
				.title("title")
				.popupCategory(PopupCategory.ART)
				.status(PopupStatus.REQUEST)
				.build();
		assertThat(created.getPopupId()).isEqualTo(popupId);

		PopupDetailDto detail = PopupDetailDto.builder()
				.popupId(popupId)
				.storeId(UUID.randomUUID())
				.title("title")
				.popupCategory(PopupCategory.FOOD)
				.status(PopupStatus.OPEN)
				.schedules(List.of())
				.build();
		assertThat(detail.getStatus()).isEqualTo(PopupStatus.OPEN);

		PopupListDto list = PopupListDto.builder()
				.popupId(popupId)
				.title("title")
				.popupCategory(PopupCategory.FOOD)
				.status(PopupStatus.OPEN)
				.build();
		assertThat(list.getPopupCategory()).isEqualTo(PopupCategory.FOOD);

		PopupUpdatedDto updated = PopupUpdatedDto.builder()
				.popupId(popupId)
				.title("updated")
				.popupCategory(PopupCategory.FASHION)
				.status(PopupStatus.CLOSED)
				.build();
		assertThat(updated.getTitle()).isEqualTo("updated");

		PopupStatusUpdatedDto statusUpdated = PopupStatusUpdatedDto.builder()
				.popupId(popupId)
				.title("title")
				.status(PopupStatus.CLOSED)
				.updatedBy(1L)
				.updatedAt(LocalDateTime.now())
				.build();
		assertThat(statusUpdated.getStatus()).isEqualTo(PopupStatus.CLOSED);

		PopupDeletedDto deleted = PopupDeletedDto.builder()
				.popupId(popupId)
				.deletedAt(LocalDateTime.now())
				.build();
		assertThat(deleted.getPopupId()).isEqualTo(popupId);

		PopupScheduleCreatedDto schedule = new PopupScheduleCreatedDto();
		assertThat(schedule).isNotNull();

		CreatePopupScheduleRequest request = CreatePopupScheduleRequest.builder()
				.startAt(LocalDateTime.now())
				.endAt(LocalDateTime.now().plusHours(1))
				.price(1000)
				.capacity(10)
				.build();
		assertThat(request.getPrice()).isEqualTo(1000);
	}

	@Test
	@DisplayName("오너 팝업 예외 팩토리가 응답 코드를 포함한다")
	void ownerPopupExceptions() {
		OwnerPopupException ex = OwnerPopupException.of(OwnerPopupResponseCode.INVALID_ROLE);
		assertThat(ex.getResponseCode()).isEqualTo(OwnerPopupResponseCode.INVALID_ROLE);

		assertThat(OwnerPopupException.unauthenticated().getResponseCode())
				.isEqualTo(OwnerPopupResponseCode.UNAUTHENTICATED);
	}
}
