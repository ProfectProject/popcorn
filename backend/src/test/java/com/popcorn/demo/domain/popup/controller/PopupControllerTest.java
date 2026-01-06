package com.popcorn.demo.domain.popup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;

class PopupControllerTest extends PopupControllerTestBase {

	@Test
	@DisplayName("팝업 목록 조회")
	void getPopups() throws Exception {
		PopupListResponse response = createPopupListResponse();

		when(popupService.getPopups(any(PopupListQuery.class)))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popups")
						.param("category", "FOOD")
						.param("page", "1")
						.param("size", "20")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value("테스트 팝업"));
	}

	@Test
	@DisplayName("팝업 상세 조회")
	void getPopupDetail() throws Exception {
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		PopupDetailResponse response = createPopupDetailResponse(productId);

		when(popupService.getPopupDetail(any(PopupDetailQuery.class))).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popups/{productId}", productId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(productId.toString()));
	}
}
