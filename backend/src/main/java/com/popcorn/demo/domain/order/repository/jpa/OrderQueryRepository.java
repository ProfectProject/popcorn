package com.popcorn.demo.domain.order.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.repository.view.OrderAddressView;
import com.popcorn.demo.domain.order.repository.view.OrderDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderItemDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderPaymentView;
import com.popcorn.demo.domain.order.repository.view.OrderStatusView;
import com.popcorn.demo.domain.order.repository.view.OrderTimelineView;
import com.popcorn.demo.domain.order.repository.view.StoreOrderReservationView;

public interface OrderQueryRepository extends Repository<Order, UUID> {

	@Query(value = """
			SELECT o.id AS orderId,
			       o.order_no AS orderNo,
			       o.order_type AS orderType,
			       o.status AS status,
			       o.customer_id AS customerId,
			       u.role AS customerRole,
			       u.phone AS customerPhone,
			       o.store_id AS storeId,
			       s.owner_id AS storeOwnerId,
			       o.product_id AS productId,
			       o.total_amount AS totalAmount,
			       o.cancelable_until AS cancelableUntil,
			       o.created_at AS createdAt,
			       o.updated_at AS updatedAt
			  FROM p_orders o
			  JOIN p_users u ON u.id = o.customer_id
			  JOIN p_stores s ON s.id = o.store_id
			 WHERE o.id = :orderId
			   AND o.deleted_at IS NULL
			""", nativeQuery = true)
	OrderDetailView findOrderDetail(@Param("orderId") UUID orderId);

	@Query(value = """
			SELECT oi.id AS orderItemId,
			       oi.order_item_type AS orderItemType,
			       oi.session_option_id AS sessionOptionId,
			       oi.merch_variant_id AS merchVariantId,
			       oi.qty AS qty,
			       oi.unit_price AS unitPrice,
			       oi.line_amount AS lineAmount,
			       so.session_id AS sessionId,
			       ps.start_at AS sessionStartAt,
			       ps.end_at AS sessionEndAt,
			       mv.name AS merchVariantName,
			       mv.sku AS merchSku,
			       p.id AS productId,
			       p.title AS productTitle,
			       p.category AS productCategory,
			       p.status AS productStatus
			  FROM p_order_items oi
			  LEFT JOIN p_session_options so ON oi.session_option_id = so.id
			  LEFT JOIN p_product_sessions ps ON so.session_id = ps.id
			  LEFT JOIN p_merch_variants mv ON oi.merch_variant_id = mv.id
			  LEFT JOIN p_products p ON p.id = COALESCE(ps.product_id, mv.product_id, :fallbackProductId)
			 WHERE oi.order_id = :orderId
			   AND oi.deleted_at IS NULL
			""", nativeQuery = true)
	List<OrderItemDetailView> findOrderItems(@Param("orderId") UUID orderId,
			@Param("fallbackProductId") UUID fallbackProductId);

	@Query(value = """
			SELECT ua.address1 AS address1,
			       ua.address2 AS address2,
			       ua.name AS receiverName,
			       u.phone AS phone
			  FROM p_user_addresses ua
			  JOIN p_users u ON u.id = ua.user_id
			 WHERE ua.user_id = :userId
			   AND ua.is_default = TRUE
			   AND ua.deleted_at IS NULL
			 ORDER BY ua.created_at DESC
			 LIMIT 1
			""", nativeQuery = true)
	OrderAddressView findDefaultAddress(@Param("userId") Long userId);

	@Query(value = """
			SELECT p.id AS id,
			       p.method AS method,
			       p.status AS status,
			       p.amount AS amount,
			       p.approved_at AS approvedAt
			  FROM p_payments p
			 WHERE p.order_id = :orderId
			   AND p.deleted_at IS NULL
			 LIMIT 1
			""", nativeQuery = true)
	OrderPaymentView findPayment(@Param("orderId") UUID orderId);

	@Query(value = """
			SELECT o.id AS orderId,
			       o.order_no AS orderNo,
			       o.status AS status,
			       p.status AS paymentStatus,
			       o.cancelable_until AS cancelableUntil,
			       o.updated_at AS updatedAt
			  FROM p_orders o
			  LEFT JOIN p_payments p ON p.order_id = o.id AND p.deleted_at IS NULL
			 WHERE o.deleted_at IS NULL
			   AND o.id = :orderId
			   AND o.customer_id = :customerId
			""", nativeQuery = true)
	OrderStatusView findOrderStatus(@Param("orderId") UUID orderId,
			@Param("customerId") Long customerId);

	@Query(value = """
			SELECT o.id AS orderId,
			       o.order_no AS orderNo,
			       o.status AS status,
			       p.status AS paymentStatus,
			       o.cancelable_until AS cancelableUntil,
			       o.updated_at AS updatedAt
			  FROM p_orders o
			  LEFT JOIN p_payments p ON p.order_id = o.id AND p.deleted_at IS NULL
			 WHERE o.deleted_at IS NULL
			   AND o.id = :orderId
			""", nativeQuery = true)
	OrderStatusView findOrderStatusByOrderId(@Param("orderId") UUID orderId);

	@Query(value = """
			SELECT COUNT(1)
			  FROM p_orders o
			 WHERE o.deleted_at IS NULL
			   AND (:storeId IS NULL OR o.store_id = :storeId)
			   AND (:productId IS NULL OR o.product_id = :productId)
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			""", nativeQuery = true)
	long countStoreOrders(@Param("storeId") UUID storeId,
			@Param("productId") UUID productId,
			@Param("status") String status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

	@Query(value = """
			SELECT o.id AS id,
			       o.order_no AS orderNo,
			       o.status AS status,
			       o.total_amount AS totalAmount,
			       o.cancelable_until AS cancelableUntil,
			       o.created_at AS createdAt
			  FROM p_orders o
			 WHERE o.deleted_at IS NULL
			   AND (:storeId IS NULL OR o.store_id = :storeId)
			   AND (:productId IS NULL OR o.product_id = :productId)
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			 ORDER BY o.created_at DESC
			 LIMIT :limit OFFSET :offset
			""", nativeQuery = true)
	List<StoreOrderReservationView> findStoreOrders(@Param("storeId") UUID storeId,
			@Param("productId") UUID productId,
			@Param("status") String status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			@Param("limit") int limit,
			@Param("offset") long offset);

	@Query(value = """
			SELECT COUNT(1)
			  FROM p_orders o
			 WHERE o.deleted_at IS NULL
			   AND o.customer_id = :customerId
			   AND (:orderType IS NULL OR o.order_type = :orderType)
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			""", nativeQuery = true)
	long countCustomerOrders(@Param("customerId") Long customerId,
			@Param("orderType") String orderType,
			@Param("status") String status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

	@Query(value = """
			SELECT o.id AS id,
			       o.order_no AS orderNo,
			       o.order_type AS orderType,
			       o.status AS status,
			       o.total_amount AS totalAmount,
			       o.cancelable_until AS cancelableUntil,
			       o.created_at AS createdAt,
			       o.product_id AS productId,
			       o.store_id AS storeId,
			       p.title AS productTitle,
			       MIN(ps.start_at) AS sessionStartAt,
			       MAX(pl.name) AS locationName,
			       MAX(pl.address1) AS locationAddress1,
			       MAX(pl.address2) AS locationAddress2
			  FROM p_orders o
			  LEFT JOIN p_products p ON p.id = o.product_id
			  LEFT JOIN p_product_locations pl ON pl.product_id = p.id AND pl.deleted_at IS NULL
			  LEFT JOIN p_order_items oi ON oi.order_id = o.id AND oi.deleted_at IS NULL
			  LEFT JOIN p_session_options so ON oi.session_option_id = so.id
			  LEFT JOIN p_product_sessions ps ON so.session_id = ps.id
			 WHERE o.deleted_at IS NULL
			   AND o.customer_id = :customerId
			   AND (:orderType IS NULL OR o.order_type = :orderType)
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			 GROUP BY o.id, o.order_no, o.order_type, o.status, o.total_amount,
			          o.cancelable_until, o.created_at, o.product_id, o.store_id, p.title
			 ORDER BY o.created_at DESC
			 LIMIT :limit OFFSET :offset
			""", nativeQuery = true)
	List<OrderTimelineView> findCustomerOrders(@Param("customerId") Long customerId,
			@Param("orderType") String orderType,
			@Param("status") String status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			@Param("limit") int limit,
			@Param("offset") long offset);

	@Query(value = """
			SELECT COUNT(1)
			  FROM p_managers_store
			 WHERE user_id = :userId
			   AND store_id = :storeId
			   AND COALESCE(is_user_stop, FALSE) = FALSE
			   AND COALESCE(is_owner_stop, FALSE) = FALSE
			   AND COALESCE(is_force_stop, FALSE) = FALSE
			""", nativeQuery = true)
	long countStoreManager(@Param("userId") Long userId,
			@Param("storeId") UUID storeId);
}
