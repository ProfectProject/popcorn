package com.popcorn.demo.domain.popup.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@Sql(scripts = "classpath:sql/test-schema.sql")
class OwnerPopupControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("오너 팝업 생성/상세 조회 전체 흐름")
	void 오너_팝업_생성_상세_조회_전체_흐름() throws Exception {
		Long ownerId = 2001L;
		UUID storeId = createStore(ownerId, "controller-store");

		String requestJson = """
				{
				  "storeId": "%s",
				  "title": "컨트롤러 팝업",
				  "description": "설명",
				  "category": "FOOD",
				  "schedules": [
				    {
				      "startAt": "%s",
				      "endAt": "%s",
				      "price": 10000,
				      "capacity": 30
				    }
				  ]
				}
				""".formatted(storeId,
				LocalDateTime.now().plusDays(1),
				LocalDateTime.now().plusDays(2));

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/owner/stores/popups")
						.with(user(ownerId.toString()).roles("OWNER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("REQUEST"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode data = objectMapper.readTree(response).get("data");
		UUID popupId = UUID.fromString(data.get("popupId").asText());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores/popups/{popupId}", popupId)
						.with(user(ownerId.toString()).roles("OWNER")))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.popupId").value(popupId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.schedules[0].capacity").value(30));

		Integer scheduleCount = jdbcTemplate.queryForObject(
				"select count(*) from p_popup_schedules where popup_id = ? and deleted_at is null",
				Integer.class,
				popupId);
		assertThat(scheduleCount).isEqualTo(1);
	}

	private UUID createStore(Long ownerId, String name) {
		ensureOwner(ownerId);
		UUID storeId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"insert into p_stores (store_id, user_id, store_name, status, reason, deleted_at, deleted_by, created_at, updated_at, created_by, updated_by) "
						+ "values (?, ?, ?, ?, null, null, null, ?, ?, ?, ?)",
				storeId, ownerId, name, "ACTIVE", now, now, ownerId, ownerId);
		return storeId;
	}

	private void ensureOwner(Long ownerId) {
		LocalDateTime now = LocalDateTime.now();
		Integer existing = jdbcTemplate.queryForObject(
				"select count(*) from p_users where user_id = ?",
				Integer.class,
				ownerId);
		if (existing != null && existing > 0) {
			return;
		}
		jdbcTemplate.update(
				"insert into p_users (user_id, email, password, name, role, is_active, created_at, updated_at) "
						+ "values (?, ?, ?, ?, ?, true, ?, ?)",
				ownerId,
				"owner_" + ownerId + "@test.com",
				"password",
				"테스트 오너",
				"OWNER",
				now,
				now);
	}
}
