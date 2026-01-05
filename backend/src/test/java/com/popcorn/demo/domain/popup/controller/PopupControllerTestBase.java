package com.popcorn.demo.domain.popup.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.service.PopupService;
import com.popcorn.demo.global.config.CommonConfig;

public abstract class PopupControllerTestBase {

	protected MockMvc mockMvc;
	protected PopupService popupService;
	protected ObjectMapper objectMapper;

	@BeforeEach
	void setUpBase() {
		popupService = Mockito.mock(PopupService.class);
		objectMapper = new CommonConfig().objectMapper();
		mockMvc = MockMvcBuilders.standaloneSetup(
						new PopupController(popupService),
						new PopupSessionController(popupService))
				.setControllerAdvice(new PopupExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
		Mockito.reset(popupService);
	}

	protected PopupListResponse createPopupListResponse() {
		return PopupListResponse.builder()
				.items(List.of(PopupListResponse.ItemDto.builder()
						.id(UUID.fromString("00000000-0000-0000-0000-000000000101"))
						.storeId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
						.title("테스트 팝업")
						.productType("RESERVATION")
						.category("POPUP")
						.regionId(101L)
						.isHidden(false)
						.build()))
				.page(1)
				.size(20)
				.total(1)
				.build();
	}

	protected PopupDetailResponse createPopupDetailResponse(UUID productId) {
		return PopupDetailResponse.builder()
				.id(productId)
				.storeId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
				.title("테스트 팝업")
				.productType("RESERVATION")
				.category("POPUP")
				.regionId(101L)
				.isHidden(false)
				.build();
	}

	protected com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse createSessionListResponse() {
		return com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse.builder()
				.items(List.of(com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse.ItemDto.builder()
						.id(UUID.fromString("00000000-0000-0000-0000-000000000201"))
						.startAt(java.time.LocalDateTime.of(2025, 1, 1, 10, 0))
						.endAt(java.time.LocalDateTime.of(2025, 1, 5, 18, 0))
						.status("OPEN")
						.location(com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse.LocationDto.builder()
								.id(UUID.fromString("00000000-0000-0000-0000-000000009001"))
								.name("팝업 테스트 장소")
								.address1("서울특별시 강남구 테헤란로 123")
								.address2("ABC빌딩 12층")
								.latitude(37.498)
								.longitude(127.027)
								.build())
						.build()))
				.build();
	}
}
