package com.popcorn.demo.domain.popup.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.popup.entity.PopupSessionOption;
import com.popcorn.demo.domain.popup.repository.view.PopupOptionView;

public interface PopupOptionQueryRepository extends Repository<PopupSessionOption, UUID> {

			@Query(value = """
			SELECT CAST(so.id AS VARCHAR) AS id,
			       so.name AS name,
			       so.price AS price,
			       so.capacity AS capacity,
			       so.is_hidden AS isHidden
			  FROM p_session_options so
			  JOIN p_product_sessions ps ON ps.id = so.session_id AND ps.deleted_at IS NULL
			 WHERE so.deleted_at IS NULL
			   AND ps.product_id = :productId
			 ORDER BY so.created_at ASC
			""", nativeQuery = true)
	List<PopupOptionView> findProductOptions(@Param("productId") UUID productId);
}
