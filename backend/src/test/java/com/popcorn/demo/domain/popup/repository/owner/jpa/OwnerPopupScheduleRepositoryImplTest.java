package com.popcorn.demo.domain.popup.repository.owner.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.popup.repository.owner.view.OwnerPopupScheduleView;

class OwnerPopupScheduleRepositoryImplTest {

	@Test
	@DisplayName("스케줄 저장/수정/삭제가 위임된다")
	void scheduleDelegation() {
		JpaOwnerPopupScheduleRepository jpa = Mockito.mock(JpaOwnerPopupScheduleRepository.class);
		OwnerPopupScheduleRepositoryImpl repo = new OwnerPopupScheduleRepositoryImpl(jpa);

		UUID scheduleId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();

		repo.insertSchedule(scheduleId, popupId, now, now.plusHours(1), 1000, 10, 10, false, now, 1L, 1L);
		verify(jpa).insertSchedule(scheduleId, popupId, now, now.plusHours(1), 1000, 10, 10, false, now, 1L, 1L);

		when(jpa.updateSchedule(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
		assertThat(repo.updateSchedule(scheduleId, popupId, now, now.plusHours(1), 1000, 10, true, now, 1L))
				.isEqualTo(1);

		when(jpa.softDeleteSchedule(scheduleId, popupId, now, 1L)).thenReturn(1);
		assertThat(repo.softDeleteSchedule(scheduleId, popupId, now, 1L)).isEqualTo(1);

		when(jpa.softDeleteSchedulesByPopup(popupId, now, 1L)).thenReturn(2);
		assertThat(repo.softDeleteSchedulesByPopup(popupId, now, 1L)).isEqualTo(2);

		when(jpa.deactivateActiveSchedulesByPopup(popupId, now, 1L)).thenReturn(3);
		assertThat(repo.deactivateActiveSchedulesByPopup(popupId, now, 1L)).isEqualTo(3);
	}

	@Test
	@DisplayName("스케줄 조회/존재 확인이 위임된다")
	void scheduleQueries() {
		JpaOwnerPopupScheduleRepository jpa = Mockito.mock(JpaOwnerPopupScheduleRepository.class);
		OwnerPopupScheduleRepositoryImpl repo = new OwnerPopupScheduleRepositoryImpl(jpa);
		UUID popupId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();

		when(jpa.findSchedulesByPopup(popupId)).thenReturn(List.of(Mockito.mock(OwnerPopupScheduleView.class)));
		when(jpa.existsSchedule(scheduleId, popupId)).thenReturn(true);

		assertThat(repo.findSchedulesByPopup(popupId)).hasSize(1);
		assertThat(repo.existsSchedule(scheduleId, popupId)).isTrue();
	}
}
