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
import com.popcorn.demo.domain.popup.application.PopupApplicationService;
import com.popcorn.demo.global.config.CommonConfig;

public abstract class PopupControllerTestBase {

	protected MockMvc mockMvc;
protected PopupApplicationService popupApplicationService;
	protected ObjectMapper objectMapper;

	@BeforeEach
	void setUpBase() {
		popupApplicationService = Mockito.mock(PopupApplicationService.class);
		objectMapper = new CommonConfig().objectMapper();
		mockMvc = MockMvcBuilders.standaloneSetup(new PopupController(popupApplicationService))
				.setControllerAdvice(new PopupExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
		Mockito.reset(popupApplicationService);
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
}
