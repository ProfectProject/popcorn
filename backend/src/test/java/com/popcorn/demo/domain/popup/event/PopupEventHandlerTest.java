package com.popcorn.demo.domain.popup.event;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;

class PopupEventHandlerTest {

	@Test
	@DisplayName("Popup 이벤트 핸들러는 예외 없이 처리한다")
	void handlesPopupEvents() {
		PopupEventHandler handler = new PopupEventHandler();

		Popup popup = Popup.builder()
				.storeId(UUID.randomUUID())
				.title("테스트 팝업")
				.description("테스트 설명")
				.category(PopupCategory.FOOD)
				.status(PopupStatus.OPEN)
				.build();
		popup.setId(UUID.randomUUID());

		assertThatCode(() -> handler.handleCreated(new PopupCreatedEvent(1L, popup))).doesNotThrowAnyException();
		assertThatCode(() -> handler.handleUpdated(new PopupUpdatedEvent(1L, popup))).doesNotThrowAnyException();
		assertThatCode(() -> handler.handleStatusUpdated(new PopupStatusUpdatedEvent(1L, popup))).doesNotThrowAnyException();
		assertThatCode(() -> handler.handleDeleted(new PopupDeletedEvent(1L, popup))).doesNotThrowAnyException();
		LocalDateTime startAt = LocalDateTime.of(2025, 1, 1, 10, 0);
		LocalDateTime endAt = LocalDateTime.of(2025, 1, 1, 12, 0);
		assertThatCode(() -> handler.handleScheduleCreated(
				new PopupScheduleCreatedEvent(1L, popup.getId(), UUID.randomUUID(), startAt, endAt, 10000, 50)))
				.doesNotThrowAnyException();
		assertThatCode(() -> handler.handleScheduleUpdated(
				new PopupScheduleUpdatedEvent(1L, popup.getId(), UUID.randomUUID(), startAt, endAt, 12000, 40, true)))
				.doesNotThrowAnyException();
		assertThatCode(() -> handler.handleScheduleDeleted(
				new PopupScheduleDeletedEvent(1L, popup.getId(), UUID.randomUUID())))
				.doesNotThrowAnyException();
	}
}
