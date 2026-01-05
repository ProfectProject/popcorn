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
			SELECT CAST(ps.id AS VARCHAR) AS id,
			       ps.start_at AS startAt,
			       ps.end_at AS endAt,
			       ps.status AS status,
			       CAST(pl.id AS VARCHAR) AS locationId,
			       pl.name AS locationName,
			       pl.address1 AS locationAddress1,
			       pl.address2 AS locationAddress2,
			       pl.latitude AS locationLatitude,
			       pl.longitude AS locationLongitude
			  FROM p_product_sessions ps
			  -- 회차 전용 장소가 없을 때 최신 상품 장소를 내려주기 위한 fallback
			  LEFT JOIN p_product_locations pl
			         ON pl.product_id = ps.product_id
			        AND pl.deleted_at IS NULL
			        AND pl.created_at = (
			            SELECT MAX(pl2.created_at)
			              FROM p_product_locations pl2
			             WHERE pl2.product_id = ps.product_id
			               AND pl2.deleted_at IS NULL
			        )
			 WHERE ps.deleted_at IS NULL
			   AND ps.product_id = :productId
			   AND (CAST(:from AS TIMESTAMP) IS NULL OR ps.start_at >= CAST(:from AS TIMESTAMP))
			   AND (CAST(:to AS TIMESTAMP) IS NULL OR ps.end_at <= CAST(:to AS TIMESTAMP))
			 ORDER BY ps.start_at ASC
			""", nativeQuery = true)
	List<PopupSessionView> findProductSessions(@Param("productId") UUID productId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
}
