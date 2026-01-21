package com.popcorn.store.domain.popup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.store.domain.popup.dto.PopupResponseCode;
import com.popcorn.store.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.store.domain.popup.dto.query.PopupListQuery;
import com.popcorn.store.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.store.domain.popup.exception.PopupException;

class PopupValidationServiceTest {

	private final PopupValidationService service = new PopupValidationService();

	@Test
	@DisplayName("팝업 목록 쿼리 - null 입력이면 기본값으로 정규화")
	void normalizeListQueryWithNull() {
		PopupListQuery normalized = service.normalizeListQuery(null);

		assertThat(normalized.getPage()).isEqualTo(1);
		assertThat(normalized.getSize()).isEqualTo(20);
		assertThat(normalized.getWithTotal()).isTrue();
	}

	@Test
	@DisplayName("팝업 목록 쿼리 - 페이지가 1 미만이면 실패")
	void normalizeListQueryRejectsInvalidPage() {
		PopupListQuery query = PopupListQuery.builder()
				.page(0)
				.size(10)
				.build();

		assertThatThrownBy(() -> service.normalizeListQuery(query))
				.isInstanceOf(PopupException.class)
				.extracting("responseCode")
				.isEqualTo(PopupResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("팝업 목록 쿼리 - 사이즈가 범위를 벗어나면 실패")
	void normalizeListQueryRejectsInvalidSize() {
		PopupListQuery query = PopupListQuery.builder()
				.page(1)
				.size(101)
				.build();

		assertThatThrownBy(() -> service.normalizeListQuery(query))
				.isInstanceOf(PopupException.class)
				.extracting("responseCode")
				.isEqualTo(PopupResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("팝업 목록 쿼리 - regionId가 0 이하이면 실패")
	void normalizeListQueryRejectsInvalidRegionId() {
		PopupListQuery query = PopupListQuery.builder()
				.page(1)
				.size(10)
				.regionId(0L)
				.build();

		assertThatThrownBy(() -> service.normalizeListQuery(query))
				.isInstanceOf(PopupException.class)
				.extracting("responseCode")
				.isEqualTo(PopupResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("팝업 상세 쿼리 - popupId 없으면 실패")
	void validateDetailQueryRejectsNull() {
		assertThatThrownBy(() -> service.validateDetailQuery(PopupDetailQuery.of(null)))
				.isInstanceOf(PopupException.class)
				.extracting("responseCode")
				.isEqualTo(PopupResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("팝업 회차 쿼리 - 기간 역전이면 실패")
	void normalizeSessionQueryRejectsInvalidRange() {
		PopupScheduleListQuery query = PopupScheduleListQuery.builder()
				.popupId(UUID.randomUUID())
				.from(LocalDateTime.of(2025, 1, 10, 0, 0))
				.to(LocalDateTime.of(2025, 1, 1, 0, 0))
				.build();

		assertThatThrownBy(() -> service.normalizeSessionQuery(query))
				.isInstanceOf(PopupException.class)
				.extracting("responseCode")
				.isEqualTo(PopupResponseCode.INVALID_REQUEST);
	}
}
