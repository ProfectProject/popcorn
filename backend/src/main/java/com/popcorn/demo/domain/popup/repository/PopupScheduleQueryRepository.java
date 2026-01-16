package com.popcorn.demo.domain.popup.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.popup.entity.PopupSchedule;
import com.popcorn.demo.domain.popup.repository.view.PopupScheduleView;

public interface PopupScheduleQueryRepository extends Repository<PopupSchedule, UUID> {

	@Query(value = """
			SELECT CAST(ps.schedule_id AS VARCHAR) AS scheduleId,
			       ps.start_at AS startAt,
			       ps.end_at AS endAt,
			       ps.price AS price,
			       ps.capacity AS capacity,
			       ps.remaining_capacity AS remainingCapacity,
			       ps.is_active AS isActive
			  FROM p_popup_schedules ps
			 WHERE ps.deleted_at IS NULL
			   AND ps.popup_id = :popupId
			   AND (:from IS NULL OR ps.start_at >= :from)
			   AND (:to IS NULL OR ps.end_at <= :to)
			 ORDER BY ps.start_at ASC
			""", nativeQuery = true)
	List<PopupScheduleView> findProductSessions(@Param("popupId") UUID popupId,
												@Param("from") LocalDateTime from,
												@Param("to") LocalDateTime to);
}
