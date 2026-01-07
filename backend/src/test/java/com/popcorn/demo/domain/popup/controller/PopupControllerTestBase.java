package com.popcorn.demo.domain.popup.controller;

import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.popup.dto.query.response.PopupScheduleListResponse;
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
						new PopupScheduleController(popupService))
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
						.category("FOOD")
						.status("OPEN")
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
				.description("테스트 설명")
				.category("FOOD")
				.status("OPEN")
				.build();
	}

	protected PopupScheduleListResponse createSessionListResponse() {
		return PopupScheduleListResponse.builder()
				.items(List.of(PopupScheduleListResponse.ItemDto.builder()
						.id(UUID.fromString("00000000-0000-0000-0000-000000000201"))
						.startAt(java.time.LocalDateTime.of(2025, 1, 1, 10, 0))
						.endAt(java.time.LocalDateTime.of(2025, 1, 5, 18, 0))
						.price(12000)
						.capacity(50)
						.remainingCapacity(50)
						.isActive(true)
						.build()))
				.build();
	}
}
