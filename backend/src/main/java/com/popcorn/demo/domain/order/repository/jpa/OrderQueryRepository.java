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
			SELECT o.order_id AS orderId,
			       o.order_no AS orderNo,
			       CASE
			         WHEN SUM(CASE WHEN og.schedule_id IS NOT NULL THEN 1 ELSE 0 END) > 0 THEN 'RESERVATION'
			         WHEN SUM(CASE WHEN og.goods_variant_id IS NOT NULL THEN 1 ELSE 0 END) > 0 THEN 'PURCHASE'
			         ELSE NULL
			       END AS orderType,
			       o.status AS status,
			       o.user_id AS customerId,
			       u.role AS customerRole,
			       u.phone AS customerPhone,
			       o.store_id AS storeId,
			       s.user_id AS storeOwnerId,
			       COALESCE(MAX(ps.popup_id), MAX(gv.popup_id)) AS popupId,
			       o.total_price AS totalAmount,
			       o.cancelable_until AS cancelableUntil,
			       o.created_at AS createdAt,
			       o.updated_at AS updatedAt
			  FROM p_orders o
			  JOIN p_users u ON u.user_id = o.user_id
			  JOIN p_stores s ON s.store_id = o.store_id
			  LEFT JOIN p_order_goods og ON og.order_id = o.order_id AND og.deleted_at IS NULL
			  LEFT JOIN p_popup_schedules ps ON ps.schedule_id = og.schedule_id AND ps.deleted_at IS NULL
			  LEFT JOIN p_goods_variants gv ON gv.goods_id = og.goods_variant_id AND gv.deleted_at IS NULL
			 WHERE o.order_id = :orderId
			   AND o.deleted_at IS NULL
			 GROUP BY o.order_id, o.order_no, o.status, o.user_id, u.role, u.phone,
			          o.store_id, s.user_id, o.total_price, o.cancelable_until, o.created_at, o.updated_at
			""", nativeQuery = true)
	OrderDetailView findOrderDetail(@Param("orderId") UUID orderId);

	@Query(value = """
			SELECT og.order_goods_id AS orderItemId,
			       CASE
			         WHEN og.schedule_id IS NOT NULL THEN 'RESERVATION'
			         WHEN og.goods_variant_id IS NOT NULL THEN 'GOODS'
			         ELSE NULL
			       END AS orderItemType,
			       og.schedule_id AS sessionOptionId,
			       og.goods_variant_id AS goodsVariantId,
			       og.qty AS qty,
			       og.unit_price AS unitPrice,
			       og.price AS lineAmount,
			       og.schedule_id AS sessionId,
			       ps.start_at AS sessionStartAt,
			       ps.end_at AS sessionEndAt,
			       gv.goods_name AS merchVariantName,
			       gv.stock_unit AS merchSku,
			       p.popup_id AS popupId,
			       p.title AS productTitle,
			       p.category AS productCategory,
			       p.status AS productStatus
			  FROM p_order_goods og
			  LEFT JOIN p_popup_schedules ps ON ps.schedule_id = og.schedule_id AND ps.deleted_at IS NULL
			  LEFT JOIN p_goods_variants gv ON gv.goods_id = og.goods_variant_id AND gv.deleted_at IS NULL
			  LEFT JOIN p_popups p ON p.popup_id = COALESCE(ps.popup_id, gv.popup_id, :fallbackPopupId)
			 WHERE og.order_id = :orderId
			   AND og.deleted_at IS NULL
			""", nativeQuery = true)
	List<OrderItemDetailView> findOrderItems(@Param("orderId") UUID orderId,
			@Param("fallbackPopupId") UUID fallbackPopupId);

	@Query(value = """
			SELECT ua.address1 AS address1,
			       ua.address2 AS address2,
			       ua.addr_name AS receiverName,
			       u.phone AS phone
			  FROM p_customer_addresses ua
			  JOIN p_users u ON u.user_id = ua.user_id
			 WHERE ua.user_id = :userId
			   AND ua.is_default = TRUE
			   AND ua.deleted_at IS NULL
			 ORDER BY ua.created_at DESC
			 LIMIT 1
			""", nativeQuery = true)
	OrderAddressView findDefaultAddress(@Param("userId") Long userId);

	@Query(value = """
			SELECT p.payment_id AS paymentId,
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
			SELECT o.order_id AS orderId,
			       o.order_no AS orderNo,
			       COALESCE(h.to_status, o.status) AS status,
			       p.status AS paymentStatus,
			       o.cancelable_until AS cancelableUntil,
			       COALESCE(h.changed_at, o.updated_at) AS updatedAt
			  FROM p_orders o
			  LEFT JOIN p_payments p ON p.order_id = o.order_id AND p.deleted_at IS NULL
			  LEFT JOIN p_order_status_histories h ON h.order_id = o.order_id
			    AND h.changed_at = (
			      SELECT MAX(h2.changed_at)
			        FROM p_order_status_histories h2
			       WHERE h2.order_id = o.order_id
			    )
			 WHERE o.deleted_at IS NULL
			   AND o.order_id = :orderId
			   AND o.user_id = :customerId
			""", nativeQuery = true)
	OrderStatusView findOrderStatus(@Param("orderId") UUID orderId,
			@Param("customerId") Long customerId);

	@Query(value = """
			SELECT o.order_id AS orderId,
			       o.order_no AS orderNo,
			       COALESCE(h.to_status, o.status) AS status,
			       p.status AS paymentStatus,
			       o.cancelable_until AS cancelableUntil,
			       COALESCE(h.changed_at, o.updated_at) AS updatedAt
			  FROM p_orders o
			  LEFT JOIN p_payments p ON p.order_id = o.order_id AND p.deleted_at IS NULL
			  LEFT JOIN p_order_status_histories h ON h.order_id = o.order_id
			    AND h.changed_at = (
			      SELECT MAX(h2.changed_at)
			        FROM p_order_status_histories h2
			       WHERE h2.order_id = o.order_id
			    )
			 WHERE o.deleted_at IS NULL
			   AND o.order_id = :orderId
			""", nativeQuery = true)
	OrderStatusView findOrderStatusByOrderId(@Param("orderId") UUID orderId);

	@Query(value = """
			SELECT COUNT(1)
			  FROM p_orders o
			 WHERE o.deleted_at IS NULL
			   AND (:storeId IS NULL OR o.store_id = :storeId)
			   AND (:popupId IS NULL OR EXISTS (
			        SELECT 1
			          FROM p_order_goods og
			          LEFT JOIN p_popup_schedules ps ON ps.schedule_id = og.schedule_id AND ps.deleted_at IS NULL
			          LEFT JOIN p_goods_variants gv ON gv.goods_id = og.goods_variant_id AND gv.deleted_at IS NULL
			         WHERE og.order_id = o.order_id
			           AND og.deleted_at IS NULL
			           AND COALESCE(ps.popup_id, gv.popup_id) = :popupId
			   ))
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			""", nativeQuery = true)
	long countStoreOrders(@Param("storeId") UUID storeId,
			@Param("popupId") UUID popupId,
			@Param("status") String status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

	@Query(value = """
			SELECT o.order_id AS id,
			       o.order_no AS orderNo,
			       o.status AS status,
			       o.total_price AS totalAmount,
			       o.cancelable_until AS cancelableUntil,
			       o.created_at AS createdAt
			  FROM p_orders o
			 WHERE o.deleted_at IS NULL
			   AND (:storeId IS NULL OR o.store_id = :storeId)
			   AND (:popupId IS NULL OR EXISTS (
			        SELECT 1
			          FROM p_order_goods og
			          LEFT JOIN p_popup_schedules ps ON ps.schedule_id = og.schedule_id AND ps.deleted_at IS NULL
			          LEFT JOIN p_goods_variants gv ON gv.goods_id = og.goods_variant_id AND gv.deleted_at IS NULL
			         WHERE og.order_id = o.order_id
			           AND og.deleted_at IS NULL
			           AND COALESCE(ps.popup_id, gv.popup_id) = :popupId
			   ))
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			 ORDER BY o.created_at DESC
			 LIMIT :limit OFFSET :offset
			""", nativeQuery = true)
	List<StoreOrderReservationView> findStoreOrders(@Param("storeId") UUID storeId,
			@Param("popupId") UUID popupId,
			@Param("status") String status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			@Param("limit") int limit,
			@Param("offset") long offset);

	@Query(value = """
			SELECT COUNT(1)
			  FROM p_orders o
			 WHERE o.deleted_at IS NULL
			   AND o.user_id = :customerId
			   AND (
			        :orderType IS NULL
			        OR (:orderType = 'RESERVATION' AND EXISTS (
			             SELECT 1
			               FROM p_order_goods og
			              WHERE og.order_id = o.order_id
			                AND og.deleted_at IS NULL
			                AND og.schedule_id IS NOT NULL
			        ))
			        OR (:orderType = 'PURCHASE' AND EXISTS (
			             SELECT 1
			               FROM p_order_goods og
			              WHERE og.order_id = o.order_id
			                AND og.deleted_at IS NULL
			                AND og.goods_variant_id IS NOT NULL
			        ))
			   )
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
			SELECT o.order_id AS id,
			       o.order_no AS orderNo,
			       CASE
			         WHEN SUM(CASE WHEN og.schedule_id IS NOT NULL THEN 1 ELSE 0 END) > 0 THEN 'RESERVATION'
			         WHEN SUM(CASE WHEN og.goods_variant_id IS NOT NULL THEN 1 ELSE 0 END) > 0 THEN 'PURCHASE'
			         ELSE NULL
			       END AS orderType,
			       o.status AS status,
			       o.total_price AS totalAmount,
			       o.cancelable_until AS cancelableUntil,
			       o.created_at AS createdAt,
			       COALESCE(MAX(ps.popup_id), MAX(gv.popup_id)) AS popupId,
			       o.store_id AS storeId,
			       MAX(p.title) AS productTitle,
			       MIN(ps.start_at) AS sessionStartAt,
			       NULL AS locationName,
			       NULL AS locationAddress1,
			       NULL AS locationAddress2
			  FROM p_orders o
			  LEFT JOIN p_order_goods og ON og.order_id = o.order_id AND og.deleted_at IS NULL
			  LEFT JOIN p_popup_schedules ps ON ps.schedule_id = og.schedule_id AND ps.deleted_at IS NULL
			  LEFT JOIN p_goods_variants gv ON gv.goods_id = og.goods_variant_id AND gv.deleted_at IS NULL
			  LEFT JOIN p_popups p ON p.popup_id = COALESCE(ps.popup_id, gv.popup_id) AND p.deleted_at IS NULL
			 WHERE o.deleted_at IS NULL
			   AND o.user_id = :customerId
			   AND (
			        :orderType IS NULL
			        OR (:orderType = 'RESERVATION' AND EXISTS (
			             SELECT 1
			               FROM p_order_goods og2
			              WHERE og2.order_id = o.order_id
			                AND og2.deleted_at IS NULL
			                AND og2.schedule_id IS NOT NULL
			        ))
			        OR (:orderType = 'PURCHASE' AND EXISTS (
			             SELECT 1
			               FROM p_order_goods og2
			              WHERE og2.order_id = o.order_id
			                AND og2.deleted_at IS NULL
			                AND og2.goods_variant_id IS NOT NULL
			        ))
			   )
			   AND (:status IS NULL OR o.status = :status)
			   AND (COALESCE(:fromDate, '1970-01-01'::TIMESTAMP) = '1970-01-01'::TIMESTAMP OR o.created_at >= :fromDate)
			   AND (COALESCE(:toDate, '9999-12-31'::TIMESTAMP) = '9999-12-31'::TIMESTAMP OR o.created_at <= :toDate)
			 GROUP BY o.order_id, o.order_no, o.status, o.total_price,
			          o.cancelable_until, o.created_at, o.store_id
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
			SELECT 0
			""", nativeQuery = true)
	long countStoreManager(@Param("userId") Long userId,
			@Param("storeId") UUID storeId);
}
