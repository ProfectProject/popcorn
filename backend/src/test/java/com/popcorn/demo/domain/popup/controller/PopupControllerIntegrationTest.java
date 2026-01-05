package com.popcorn.demo.domain.popup.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"/sql/test-schema.sql", "/sql/popup-test-data.sql"})
class PopupControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("팝업 목록 조회 통합 테스트")
	void getPopups() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popups")
						.param("category", "FOOD")
						.param("regionId", "101")
						.param("page", "1")
						.param("size", "20"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.total").value(2))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value("Popup Merch 55"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].status").value("OPEN"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[1].title").value("Seed Popup 1"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[1].status").value("OPEN"));
	}

	@Test
	@DisplayName("팝업 상세 조회 통합 테스트")
	void getPopupDetail() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popups/{productId}",
						"00000000-0000-0000-0000-000000000101"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("Seed Popup 1"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("OPEN"));
	}

	@Test
	@DisplayName("회차(슬롯) 조회 통합 테스트")
	void getProductSessions() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{productId}/sessions",
						"00000000-0000-0000-0000-000000000101")
						.param("from", "2025-01-01T00:00:00")
						.param("to", "2025-01-31T23:59:59"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].price").value(12000))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].isActive").value(true));
	}
}
