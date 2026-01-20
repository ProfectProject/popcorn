package com.popcorn.checkIns;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "classpath:sql/test-schema.sql")
class QrIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final Long USER_ID = 1001L;
	private static final UUID STORE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID POPUP_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

	@BeforeEach
	void setUp() {
		// Delete in correct order to handle foreign key constraints
		jdbcTemplate.update("DELETE FROM p_checkins");
		jdbcTemplate.update("DELETE FROM p_order_qr_codes");
		jdbcTemplate.update("DELETE FROM p_order_goods");
		jdbcTemplate.update("DELETE FROM p_payments");
		jdbcTemplate.update("DELETE FROM p_order_status_histories");
		jdbcTemplate.update("DELETE FROM p_orders");
		jdbcTemplate.update("DELETE FROM p_goods_variants");
		jdbcTemplate.update("DELETE FROM p_popup_schedules");
		jdbcTemplate.update("DELETE FROM p_popups"); // This was missing - popups reference stores
		jdbcTemplate.update("DELETE FROM p_customer_addresses");
		jdbcTemplate.update("DELETE FROM p_stores");
		jdbcTemplate.update("DELETE FROM p_users");

		insertUser(USER_ID);
		insertStore(STORE_ID, USER_ID);
		insertPopup(POPUP_ID, STORE_ID);
	}

	@Test
	@DisplayName("QR 발급 통합 테스트")
	@WithMockUser(roles = "CUSTOMER")
	void issueQr_integration() throws Exception {
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000010");
		insertOrder(orderId, USER_ID, STORE_ID, "PAID");
		insertReservationItem(orderId, POPUP_ID);

		mockMvc.perform(post("/api/qr/v1/orders/{orderId}", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.qrCode").isNotEmpty());
	}

	@Test
	@DisplayName("QR 조회 통합 테스트")
	@WithMockUser(roles = "CUSTOMER")
	void getQr_integration() throws Exception {
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000011");
		UUID qrId = UUID.fromString("80000000-0000-0000-0000-000000000011");
		insertOrder(orderId, USER_ID, STORE_ID, "PAID");
		insertReservationItem(orderId, POPUP_ID);
		insertQr(qrId, orderId, "qr-get-001", LocalDateTime.now().plusMinutes(5));

		mockMvc.perform(get("/api/qr/v1/orders/{orderId}", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.qrCode").value("qr-get-001"));
	}

	@Test
	@DisplayName("QR 검증 시 체크인 생성 통합 테스트")
	@WithMockUser(roles = "CUSTOMER")
	void verifyQr_createsCheckin_integration() throws Exception {
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000012");
		UUID qrId = UUID.fromString("80000000-0000-0000-0000-000000000012");
		insertOrder(orderId, USER_ID, STORE_ID, "PAID");
		insertReservationItem(orderId, POPUP_ID);
		insertQr(qrId, orderId, "qr-verify-001", LocalDateTime.now().plusMinutes(5));

		String requestJson = """
				{
					"qrCode": "qr-verify-001"
				}
				""";

		mockMvc.perform(post("/api/qr/v1/verify")
						.contentType("application/json")
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.valid").value(true))
				.andExpect(jsonPath("$.data.checkinId").isNotEmpty());
	}

	@Test
	@DisplayName("QR 검증 중복 호출 시 체크인 중복 생성 방지")
	@WithMockUser(roles = "CUSTOMER")
	void verifyQr_reusesCheckin_integration() throws Exception {
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000013");
		UUID qrId = UUID.fromString("80000000-0000-0000-0000-000000000013");
		insertOrder(orderId, USER_ID, STORE_ID, "PAID");
		insertReservationItem(orderId, POPUP_ID);
		insertQr(qrId, orderId, "qr-verify-002", LocalDateTime.now().plusMinutes(5));

		String requestJson = """
				{
					"qrCode": "qr-verify-002"
				}
				""";

		mockMvc.perform(post("/api/qr/v1/verify")
						.contentType("application/json")
						.content(requestJson))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/qr/v1/verify")
						.contentType("application/json")
						.content(requestJson))
				.andExpect(status().isOk());

		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM p_checkins", Integer.class);
		org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
	}

	private void insertUser(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_users (user_id, password, name, phone, email, role, is_active, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
				userId,
				"pw",
				"사용자",
				"01000000000",
				"user" + userId + "@test.com",
				"CUSTOMER",
				true,
				toTimestamp(now),
				toTimestamp(now)
		);
	}

	private void insertStore(UUID storeId, Long userId) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_stores (store_id, user_id, store_name, status, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				storeId,
				userId,
				"테스트 스토어",
				"DRAFT",
				toTimestamp(now),
				toTimestamp(now)
		);
	}

	private void insertPopup(UUID popupId, UUID storeId) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_popups (popup_id, store_id, title, description, category, status, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				popupId,
				storeId,
				"테스트 팝업",
				"설명",
				"ETC",
				"APPROVED",
				toTimestamp(now),
				toTimestamp(now)
		);
	}

	private void insertOrder(UUID orderId, Long userId, UUID storeId, String status) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_orders (order_id, order_no, user_id, store_id, status, total_price, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				orderId,
				"O20240101-000002", // 테스트용 짧은 주문번호 사용 (16자)
				userId,
				storeId,
				status,
				10000,
				toTimestamp(now),
				toTimestamp(now)
		);
	}

	private void insertReservationItem(UUID orderId, UUID popupId) {
		LocalDateTime now = LocalDateTime.now();
		UUID scheduleId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				INSERT INTO p_popup_schedules
					(schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
				scheduleId,
				popupId,
				toTimestamp(now.plusDays(1)),
				toTimestamp(now.plusDays(1).plusHours(2)),
				10000,
				10,
				10,
				true,
				toTimestamp(now),
				toTimestamp(now)
		);

		jdbcTemplate.update(
				"""
				INSERT INTO p_order_goods
					(order_goods_id, order_id, schedule_id, goods_variant_id, qty, unit_price, price, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				orderId,
				scheduleId,
				null,
				1,
				10000,
				10000,
				toTimestamp(now),
				toTimestamp(now)
		);
	}

	private void insertQr(UUID qrId, UUID orderId, String qrCode, LocalDateTime expiresAt) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_order_qr_codes (qr_id, order_id, qr_code, expires_at, created_at, created_by)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				qrId,
				orderId,
				qrCode,
				toTimestamp(expiresAt),
				toTimestamp(now),
				USER_ID
		);
	}

	private static Timestamp toTimestamp(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return Timestamp.valueOf(value);
	}
}
