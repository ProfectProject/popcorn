package com.popcorn.demo.domain.popup.dto.owner.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;

class PopupOwnerResponseDtoTest {

	@Test
	@DisplayName("Owner 팝업 응답 DTO를 생성한다")
	void buildsOwnerPopupDtos() {
		UUID popupId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();

		PopupCreatedDto createdDto = PopupCreatedDto.builder()
				.popupId(popupId)
				.storeId(storeId)
				.title("title")
				.description("desc")
				.popupCategory(PopupCategory.FOOD)
				.status(PopupStatus.OPEN)
				.createdAt(now)
				.createdBy(1L)
				.build();
		assertThat(createdDto.getPopupId()).isEqualTo(popupId);

		PopupUpdatedDto updatedDto = PopupUpdatedDto.builder()
				.popupId(popupId)
				.title("updated")
				.description("desc")
				.popupCategory(PopupCategory.ART)
				.status(PopupStatus.CLOSED)
				.updatedAt(now)
				.updatedBy(2L)
				.build();
		assertThat(updatedDto.getTitle()).isEqualTo("updated");

		PopupStatusUpdatedDto statusUpdatedDto = PopupStatusUpdatedDto.builder()
				.popupId(popupId)
				.status(PopupStatus.OPEN)
				.updatedAt(now)
				.updatedBy(3L)
				.build();
		assertThat(statusUpdatedDto.getStatus()).isEqualTo(PopupStatus.OPEN);

		PopupDeletedDto deletedDto = PopupDeletedDto.builder()
				.popupId(popupId)
				.deletedAt(now)
				.deletedBy(4L)
				.build();
		assertThat(deletedDto.getPopupId()).isEqualTo(popupId);

		PopupListDto listDto = PopupListDto.builder()
				.popupId(popupId)
				.title("list")
				.popupCategory(PopupCategory.FOOD)
				.status(PopupStatus.OPEN)
				.createdAt(now)
				.build();
		assertThat(listDto.getTitle()).isEqualTo("list");

		PopupScheduleDetailDto scheduleDetail = PopupScheduleDetailDto.builder()
				.scheduleId(UUID.randomUUID())
				.startAt(now)
				.endAt(now.plusHours(1))
				.price(10000)
				.capacity(10)
				.remainingCapacity(5)
				.active(true)
				.build();

		PopupDetailDto detailDto = PopupDetailDto.builder()
				.popupId(popupId)
				.storeId(storeId)
				.title("detail")
				.description("desc")
				.popupCategory(PopupCategory.FOOD)
				.status(PopupStatus.OPEN)
				.createdAt(now)
				.updatedAt(now)
				.schedules(List.of(scheduleDetail))
				.build();
		assertThat(detailDto.getSchedules()).hasSize(1);

		assertThat(new PopupScheduleCreatedDto()).isNotNull();
		assertThat(new PopupScheduleUpdatedDto()).isNotNull();
		assertThat(new PopupScheduleDeletedDto()).isNotNull();
		assertThat(new PopupScheduleStatusUpdatedDto()).isNotNull();
	}
}
