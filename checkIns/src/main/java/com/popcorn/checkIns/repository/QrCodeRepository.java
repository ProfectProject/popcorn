package com.popcorn.checkIns.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QrCodeRepository {

	private final JdbcTemplate jdbcTemplate;

	public Optional<String> findOrderStatus(UUID orderId) {
		List<String> statuses = jdbcTemplate.query(
				"SELECT status FROM p_orders WHERE order_id = ?",
				(rs, rowNum) -> rs.getString("status"),
				orderId
		);

		return statuses.stream().findFirst();
	}

	public Optional<QrCodeRow> findLatestByOrderId(UUID orderId) {
		List<QrCodeRow> rows = jdbcTemplate.query(
				"""
				SELECT qr_id, order_id, qr_code, expires_at, created_at
				FROM p_order_qr_codes
				WHERE order_id = ?
				ORDER BY created_at DESC
				LIMIT 1
				""",
				(rs, rowNum) -> new QrCodeRow(
						UUID.fromString(rs.getString("qr_id")),
						UUID.fromString(rs.getString("order_id")),
						rs.getString("qr_code"),
						toLocalDateTime(rs.getTimestamp("expires_at")),
						toLocalDateTime(rs.getTimestamp("created_at"))
				),
				orderId
		);

		return rows.stream().findFirst();
	}

	public Optional<QrCodeRow> findLatestByQrCode(String qrCode) {
		List<QrCodeRow> rows = jdbcTemplate.query(
				"""
				SELECT qr_id, order_id, qr_code, expires_at, created_at
				FROM p_order_qr_codes
				WHERE qr_code = ?
				ORDER BY created_at DESC
				LIMIT 1
				""",
				(rs, rowNum) -> new QrCodeRow(
						UUID.fromString(rs.getString("qr_id")),
						UUID.fromString(rs.getString("order_id")),
						rs.getString("qr_code"),
						toLocalDateTime(rs.getTimestamp("expires_at")),
						toLocalDateTime(rs.getTimestamp("created_at"))
				),
				qrCode
		);

		return rows.stream().findFirst();
	}

	public void insert(QrCodeRow row) {
		jdbcTemplate.update(
				"""
				INSERT INTO p_order_qr_codes
					(qr_id, order_id, qr_code, expires_at, created_at, created_by)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				row.qrId(),
				row.orderId(),
				row.qrCode(),
				toTimestamp(row.expiresAt()),
				toTimestamp(row.createdAt()),
				null
		);
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return timestamp.toLocalDateTime();
	}

	private static Timestamp toTimestamp(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return Timestamp.valueOf(value);
	}
}
