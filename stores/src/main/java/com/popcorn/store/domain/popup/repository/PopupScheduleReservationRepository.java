package com.popcorn.store.domain.popup.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleCapacity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PopupScheduleReservationRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public PopupScheduleCapacity reserveCapacity(UUID popupId, UUID scheduleId, int quantity) {
		String sql = """
			UPDATE p_popup_schedules
			   SET reservation_capacity = reservation_capacity + 1,
			       updated_at = now()
			 WHERE schedule_id = :scheduleId
			   AND popup_id = :popupId
			   AND deleted_at IS NULL
			   AND is_active IS TRUE
			   AND (remaining_capacity - :quantity - reservation_capacity) >= 0
			RETURNING schedule_id, capacity, remaining_capacity, reservation_capacity
			""";

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("popupId", popupId)
			.addValue("scheduleId", scheduleId)
			.addValue("quantity", quantity);

		return jdbcTemplate.query(sql, params, rs -> rs.next() ? mapCapacity(rs) : null);
	}

	public PopupScheduleCapacity cancelCapacity(UUID scheduleId, int quantity) {
		String sql = """
			UPDATE p_popup_schedules
			   SET remaining_capacity = remaining_capacity + :quantity,
			       reservation_capacity = reservation_capacity - :quantity,
			       updated_at = now()
			 WHERE schedule_id = :scheduleId
			   AND deleted_at IS NULL
			   AND is_active IS TRUE
			   AND reservation_capacity >= :quantity
			RETURNING schedule_id, capacity, remaining_capacity, reservation_capacity
			""";

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("scheduleId", scheduleId)
			.addValue("quantity", quantity);

		return jdbcTemplate.query(sql, params, rs -> rs.next() ? mapCapacity(rs) : null);
	}

	public PopupScheduleCapacity failCapacity(UUID scheduleId, int quantity) {
		String sql = """
				UPDATE p_popup_schedules
					SET reservation_capacity = reservation_capacity - :quantity,
				       updated_at = now()
				 WHERE schedule_id = :scheduleId
				   AND deleted_at IS NULL
				   AND is_active IS TRUE
				   AND reservation_capacity >= :quantity
				RETURNING schedule_id, capacity, remaining_capacity, reservation_capacity
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("scheduleId", scheduleId)
				.addValue("quantity", quantity);
		return jdbcTemplate.query(sql, params, rs -> rs.next() ? mapCapacity(rs) : null);
	}

	public PopupScheduleCapacity completeCapacity(UUID scheduleId, int quantity){
		String sql = """
				UPDATE p_popup_schedules
				   SET remaining_capacity = remaining_capacity - :quantity,
				       reservation_capacity = reservation_capacity - :quantity,
				       updated_at = now()
				 WHERE schedule_id = :scheduleId
				   AND deleted_at IS NULL
				   AND is_active IS TRUE
				   AND (remaining_capacity - :quantity) >= 0
				   AND reservation_capacity >= :quantity
				RETURNING schedule_id, capacity, remaining_capacity, reservation_capacity
				""";

		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("scheduleId", scheduleId)
				.addValue("quantity", quantity);

		return jdbcTemplate.query(sql, params, rs -> rs.next() ? mapCapacity(rs) : null);
	}

	private PopupScheduleCapacity mapCapacity(ResultSet rs) throws SQLException {
		return new PopupScheduleCapacity(
			UUID.fromString(rs.getString("schedule_id")),
			rs.getInt("capacity"),
			rs.getInt("remaining_capacity"),
			rs.getInt("reservation_capacity")
		);
	}
}
