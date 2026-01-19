package com.popcorn.demo.domain.qr.checkin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:sql/test-schema.sql")
class CheckinIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final Long USER_ID = 1001L;
	private static final UUID STORE_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

	@BeforeEach
	void setUp() {
		// 외래 키 제약 조건을 고려하여 자식 테이블부터 삭제
		jdbcTemplate.update("DELETE FROM p_checkins");
		jdbcTemplate.update("DELETE FROM p_order_qr_codes");
		jdbcTemplate.update("DELETE FROM p_order_goods");
		jdbcTemplate.update("DELETE FROM p_payments");
		jdbcTemplate.update("DELETE FROM p_order_status_histories");
		jdbcTemplate.update("DELETE FROM p_orders");
		jdbcTemplate.update("DELETE FROM p_popup_schedules");
		jdbcTemplate.update("DELETE FROM p_goods_variants");
		jdbcTemplate.update("DELETE FROM p_popups");
		jdbcTemplate.update("DELETE FROM p_stores");
		jdbcTemplate.update("DELETE FROM p_customer_addresses");
		jdbcTemplate.update("DELETE FROM p_users");

		insertUser(USER_ID);
		insertStore(STORE_ID, USER_ID);
	}

	@Test
	@DisplayName("체크인 목록 조회 통합 테스트")
	@WithMockUser(roles = "OWNER")
	void getCheckins_integration() throws Exception {
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000021");
		UUID qrId = UUID.fromString("80000000-0000-0000-0000-000000000021");
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000021");
		insertOrder(orderId, USER_ID, STORE_ID, "RESERVED");
		insertQr(qrId, orderId, "qr-list-101", LocalDateTime.now().plusMinutes(5));
		insertCheckin(checkinId, orderId, qrId);

		mockMvc.perform(get("/api/v1/checkins"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.count").value(1))
				.andExpect(jsonPath("$.data.items[0].checkinId").value(checkinId.toString()))
				.andExpect(jsonPath("$.data.items[0].orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.items[0].qrCode").value("qr-list-101"));
	}

	@Test
	@DisplayName("체크인 상세 조회 통합 테스트")
	void getCheckin_integration() throws Exception {
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000022");
		UUID qrId = UUID.fromString("80000000-0000-0000-0000-000000000022");
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000022");
		insertOrder(orderId, USER_ID, STORE_ID, "RESERVED");
		insertQr(qrId, orderId, "qr-detail-101", LocalDateTime.now().plusMinutes(5));
		insertCheckin(checkinId, orderId, qrId);

		mockMvc.perform(get("/api/v1/checkins/{checkinId}", checkinId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.checkinId").value(checkinId.toString()))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.orderQrCodeId").value(qrId.toString()))
				.andExpect(jsonPath("$.data.qrCode").value("qr-detail-101"));
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

	private void insertOrder(UUID orderId, Long userId, UUID storeId, String status) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_orders (order_id, order_no, user_id, store_id, status, total_price, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				orderId,
				"O20240101-000001", // 테스트용 짧은 주문번호 사용 (16자)
				userId,
				storeId,
				status,
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

	private void insertCheckin(UUID checkinId, UUID orderId, UUID qrId) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"""
				INSERT INTO p_checkins (checkin_id, order_id, order_qr_code_id, created_at, created_by)
				VALUES (?, ?, ?, ?, ?)
				""",
				checkinId,
				orderId,
				qrId,
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
