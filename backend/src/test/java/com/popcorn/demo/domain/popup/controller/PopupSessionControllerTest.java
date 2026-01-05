package com.popcorn.demo.domain.popup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.popup.dto.query.PopupSessionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse;

class PopupSessionControllerTest extends PopupControllerTestBase {

	@Test
	@DisplayName("회차(슬롯) 조회")
	void getProductSessions() throws Exception {
		PopupSessionListResponse response = createSessionListResponse();
		when(popupService.getProductSessions(any(PopupSessionListQuery.class)))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{productId}/sessions",
						UUID.fromString("00000000-0000-0000-0000-000000000101"))
						.param("from", "2025-01-01T00:00:00")
						.param("to", "2025-01-31T23:59:59")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].price").value(12000));
	}
}
