package com.popcorn.demo.domain.popup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.popup.entity.PopupProduct;
import com.popcorn.demo.domain.popup.repository.view.PopupListView;

public interface PopupQueryRepository extends Repository<PopupProduct, UUID> {

	@Query(value = """
			SELECT CAST(p.id AS VARCHAR) AS id,
			       CAST(p.store_id AS VARCHAR) AS storeId,
			       p.title AS title,
			       p.category AS category,
			       p.region_id AS regionId,
			       p.is_hidden AS isHidden,
			       MIN(ps.start_at) AS eventStartAt,
			       MAX(ps.end_at) AS eventEndAt,
			       CAST(pl.id AS VARCHAR) AS locationId,
			       pl.name AS locationName,
			       pl.address1 AS locationAddress1,
			       pl.address2 AS locationAddress2,
			       pl.latitude AS locationLatitude,
			       pl.longitude AS locationLongitude,
			       MIN(CASE
			         WHEN EXISTS (
			             SELECT 1
			               FROM p_merch_variants mv
			              WHERE mv.product_id = p.id
			                AND mv.deleted_at IS NULL
			         ) THEN 'MERCH'
			         WHEN EXISTS (
			             SELECT 1
			               FROM p_product_sessions ps2
			              WHERE ps2.product_id = p.id
			                AND ps2.deleted_at IS NULL
			         ) THEN 'RESERVATION'
			         ELSE 'RESERVATION'
			       END) AS productType
			  FROM p_products p
			  LEFT JOIN p_product_locations pl
			         ON pl.product_id = p.id
			        AND pl.deleted_at IS NULL
			        AND pl.created_at = (
			            SELECT MAX(pl2.created_at)
			              FROM p_product_locations pl2
			             WHERE pl2.product_id = p.id
			               AND pl2.deleted_at IS NULL
			        )
			  LEFT JOIN p_product_sessions ps ON ps.product_id = p.id AND ps.deleted_at IS NULL
			 WHERE p.deleted_at IS NULL
			   AND (:regionId IS NULL OR p.region_id = :regionId)
			   AND (:category IS NULL OR p.category = :category)
			   AND (:keyword IS NULL OR p.title ILIKE CONCAT('%', :keyword, '%') OR p.description ILIKE CONCAT('%', :keyword, '%'))
			   AND (:storeId IS NULL OR p.store_id = :storeId)
			 GROUP BY p.id, p.store_id, p.title, p.category, p.is_hidden,
			          p.region_id, pl.id, pl.name, pl.address1, pl.address2, pl.latitude, pl.longitude
			 ORDER BY p.created_at DESC
			 LIMIT :limit OFFSET :offset
			""", nativeQuery = true)
	List<PopupListView> findPopups(@Param("regionId") Long regionId,
			@Param("category") String category,
			@Param("keyword") String keyword,
			@Param("storeId") UUID storeId,
			@Param("limit") int limit,
			@Param("offset") long offset);

	@Query(value = """
			SELECT COUNT(1)
			  FROM p_products p
			 WHERE p.deleted_at IS NULL
			   AND (:regionId IS NULL OR p.region_id = :regionId)
			   AND (:category IS NULL OR p.category = :category)
			   AND (:keyword IS NULL OR p.title ILIKE CONCAT('%', :keyword, '%') OR p.description ILIKE CONCAT('%', :keyword, '%'))
			   AND (:storeId IS NULL OR p.store_id = :storeId)
			""", nativeQuery = true)
	long countPopups(@Param("regionId") Long regionId,
			@Param("category") String category,
			@Param("keyword") String keyword,
			@Param("storeId") UUID storeId);

	@Query(value = """
			SELECT CAST(p.id AS VARCHAR) AS id,
			       CAST(p.store_id AS VARCHAR) AS storeId,
			       p.title AS title,
			       p.category AS category,
			       p.region_id AS regionId,
			       p.is_hidden AS isHidden,
			       MIN(ps.start_at) AS eventStartAt,
			       MAX(ps.end_at) AS eventEndAt,
			       CAST(pl.id AS VARCHAR) AS locationId,
			       pl.name AS locationName,
			       pl.address1 AS locationAddress1,
			       pl.address2 AS locationAddress2,
			       pl.latitude AS locationLatitude,
			       pl.longitude AS locationLongitude,
			       MIN(CASE
			         WHEN EXISTS (
			             SELECT 1
			               FROM p_merch_variants mv
			              WHERE mv.product_id = p.id
			                AND mv.deleted_at IS NULL
			         ) THEN 'MERCH'
			         WHEN EXISTS (
			             SELECT 1
			               FROM p_product_sessions ps2
			              WHERE ps2.product_id = p.id
			                AND ps2.deleted_at IS NULL
			         ) THEN 'RESERVATION'
			         ELSE 'RESERVATION'
			       END) AS productType
			  FROM p_products p
			  LEFT JOIN p_product_locations pl
			         ON pl.product_id = p.id
			        AND pl.deleted_at IS NULL
			        AND pl.created_at = (
			            SELECT MAX(pl2.created_at)
			              FROM p_product_locations pl2
			             WHERE pl2.product_id = p.id
			               AND pl2.deleted_at IS NULL
			        )
			  LEFT JOIN p_product_sessions ps ON ps.product_id = p.id AND ps.deleted_at IS NULL
			 WHERE p.deleted_at IS NULL
			   AND p.id = :productId
			 GROUP BY p.id, p.store_id, p.title, p.category, p.is_hidden, p.region_id,
			          pl.id, pl.name, pl.address1, pl.address2, pl.latitude, pl.longitude
			""", nativeQuery = true)
	Optional<PopupListView> findPopupDetail(@Param("productId") UUID productId);
}
