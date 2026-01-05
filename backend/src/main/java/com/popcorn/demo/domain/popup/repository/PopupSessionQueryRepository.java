package com.popcorn.demo.domain.popup.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.popup.entity.PopupSession;
import com.popcorn.demo.domain.popup.repository.view.PopupSessionView;

public interface PopupSessionQueryRepository extends Repository<PopupSession, UUID> {

	@Query(value = """
			SELECT CAST(ps.schedule_id AS VARCHAR) AS id,
			       ps.start_at AS startAt,
			       ps.end_at AS endAt,
			       ps.price AS price,
			       ps.capacity AS capacity,
			       ps.remaining_capacity AS remainingCapacity,
			       ps.is_active AS isActive
			  FROM p_popup_schedules ps
			 WHERE ps.deleted_at IS NULL
			   AND ps.popup_id = :productId
			   AND (CAST(:from AS TIMESTAMP) IS NULL OR ps.start_at >= CAST(:from AS TIMESTAMP))
			   AND (CAST(:to AS TIMESTAMP) IS NULL OR ps.end_at <= CAST(:to AS TIMESTAMP))
			 ORDER BY ps.start_at ASC
			""", nativeQuery = true)
	List<PopupSessionView> findProductSessions(@Param("productId") UUID productId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
}
