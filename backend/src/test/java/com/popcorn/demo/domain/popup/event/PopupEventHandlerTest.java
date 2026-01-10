package com.popcorn.demo.domain.popup.event;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;

class PopupEventHandlerTest {

	@Test
	@DisplayName("Popup 이벤트 핸들러는 예외 없이 처리한다")
	void handlesPopupEvents() {
		PopupEventHandler handler = new PopupEventHandler();

		PopupSearchEvent searchEvent = PopupSearchEvent.builder()
				.regionId(1L)
				.category(PopupCategory.FOOD)
				.keyword("test")
				.storeId(UUID.randomUUID())
				.page(1)
				.size(10)
				.total(3)
				.occurredAt(LocalDateTime.now())
				.build();

		PopupViewedEvent viewedEvent = PopupViewedEvent.builder()
				.popupId(UUID.randomUUID())
				.storeId(UUID.randomUUID())
				.category(PopupCategory.ART)
				.regionId(2L)
				.occurredAt(LocalDateTime.now())
				.build();

		assertThatCode(() -> handler.handleSearch(searchEvent)).doesNotThrowAnyException();
		assertThatCode(() -> handler.handleViewed(viewedEvent)).doesNotThrowAnyException();
	}
}
