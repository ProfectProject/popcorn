package com.popcorn.demo.domain.popup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupOptionListQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupSessionListQuery;
import com.popcorn.demo.domain.popup.exception.PopupException;

class PopupValidationServiceTest {

	@Test
	@DisplayName("목록 조회 - 페이지/사이즈 기본값 적용 및 최대값 제한")
	void normalizeListQuery_appliesDefaultsAndMax() {
		PopupValidationService service = new PopupValidationService();

		PopupListQuery query = PopupListQuery.builder()
				.page(0)
				.size(200)
				.build();

		PopupListQuery normalized = service.normalizeListQuery(query);

		assertEquals(1, normalized.getPage());
		assertEquals(100, normalized.getSize());
		assertEquals(true, normalized.getWithTotal());
	}

	@Test
	@DisplayName("목록 조회 - regionId가 0 이하이면 실패")
	void normalizeListQuery_rejectsInvalidRegionId() {
		PopupValidationService service = new PopupValidationService();

		PopupException exception = assertThrows(PopupException.class,
				() -> service.normalizeListQuery(PopupListQuery.builder()
						.regionId(0L)
						.build()));

		assertEquals(PopupResponseCode.INVALID_REQUEST, exception.getResponseCode());
	}

	@Test
	@DisplayName("목록 조회 - 카테고리 값 검증")
	void normalizeListQuery_rejectsInvalidCategory() {
		PopupValidationService service = new PopupValidationService();

		PopupException exception = assertThrows(PopupException.class,
				() -> service.normalizeListQuery(PopupListQuery.builder()
						.category("INVALID")
						.build()));

		assertEquals(PopupResponseCode.INVALID_REQUEST, exception.getResponseCode());
	}

	@Test
	@DisplayName("상세 조회 - productId 없으면 실패")
	void validateDetailQuery_requiresProductId() {
		PopupValidationService service = new PopupValidationService();

		PopupException exception = assertThrows(PopupException.class,
				() -> service.validateDetailQuery(PopupDetailQuery.builder().build()));

		assertEquals(PopupResponseCode.INVALID_REQUEST, exception.getResponseCode());
	}

	@Test
	@DisplayName("회차 조회 - productId 없으면 실패")
	void normalizeSessionQuery_requiresProductId() {
		PopupValidationService service = new PopupValidationService();

		PopupException exception = assertThrows(PopupException.class,
				() -> service.normalizeSessionQuery(PopupSessionListQuery.builder().build()));

		assertEquals(PopupResponseCode.INVALID_REQUEST, exception.getResponseCode());
	}

	@Test
	@DisplayName("회차 조회 - 기간이 역전되면 실패")
	void normalizeSessionQuery_rejectsInvalidDateRange() {
		PopupValidationService service = new PopupValidationService();

		PopupException exception = assertThrows(PopupException.class,
				() -> service.normalizeSessionQuery(PopupSessionListQuery.builder()
						.productId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000101"))
						.from(java.time.LocalDateTime.of(2025, 1, 10, 0, 0))
						.to(java.time.LocalDateTime.of(2025, 1, 1, 0, 0))
						.build()));

		assertEquals(PopupResponseCode.INVALID_REQUEST, exception.getResponseCode());
	}

	@Test
	@DisplayName("옵션 조회 - productId 없으면 실패")
	void normalizeOptionQuery_requiresProductId() {
		PopupValidationService service = new PopupValidationService();

		PopupException exception = assertThrows(PopupException.class,
				() -> service.normalizeOptionQuery(PopupOptionListQuery.builder().build()));

		assertEquals(PopupResponseCode.INVALID_REQUEST, exception.getResponseCode());
	}
}
