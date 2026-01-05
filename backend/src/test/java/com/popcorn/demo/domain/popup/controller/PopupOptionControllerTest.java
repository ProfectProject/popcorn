package com.popcorn.demo.domain.popup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.popup.dto.query.PopupOptionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupOptionListResponse;

class PopupOptionControllerTest extends PopupControllerTestBase {

	@Test
	@DisplayName("옵션 조회")
	void getProductOptions() throws Exception {
		PopupOptionListResponse response = createOptionListResponse();
		when(popupApplicationService.getProductOptions(any(PopupOptionListQuery.class)))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{productId}/options",
						UUID.fromString("00000000-0000-0000-0000-000000000101"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].name").value("일반 좌석"));
	}
}
