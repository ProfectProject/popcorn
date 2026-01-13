package com.popcorn.demo.domain.inventory.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.entity.OrderItem;

import lombok.RequiredArgsConstructor;

/**
 * 재고 관리 서비스
 *
 * 주문 타입별로 적절한 재고 차감을 처리합니다:
 * - 예약(RESERVATION): 스케줄별 좌석 재고 차감
 * - 상품(GOODS): 상품 재고 차감
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

	private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
	private final JdbcTemplate jdbcTemplate;

	/**
	 * 주문 완료 시 재고 차감
	 */
	@Transactional
	public void deductInventoryForOrder(UUID orderId, List<OrderItem> orderItems) {
		log.info("🔧 재고 차감 시작 - 주문ID: {}, 항목 수: {}", orderId, orderItems.size());

		for (OrderItem item : orderItems) {
			if (item.getSessionOptionId() != null) {
				// 예약 상품 - 스케줄별 좌석 차감
				deductReservationInventory(item.getSessionOptionId(), item.getQty());
			} else if (item.getGoodsVariantId() != null) {
				// 일반 상품 - 상품 재고 차감
				deductGoodsInventory(item.getGoodsVariantId(), item.getQty());
			} else {
				log.warn("⚠️ 재고 차감 불가 - 알 수 없는 주문 항목 타입: {}", item.getId());
			}
		}

		log.info("✅ 재고 차감 완료 - 주문ID: {}", orderId);
	}

	/**
	 * 현재 상품 재고 수량 조회
	 */
	public Integer getCurrentGoodsStock(UUID goodsVariantId) {
		try {
			String sql = """
				SELECT stock
				FROM p_goods_variants
				WHERE goods_id = ?
				AND deleted_at IS NULL
				AND is_active = true
				""";

			List<Integer> results = jdbcTemplate.query(sql,
				(rs, rowNum) -> rs.getInt("stock"),
				goodsVariantId);

			return results.isEmpty() ? null : results.get(0);

		} catch (Exception e) {
			log.error("❌ 상품 재고 조회 중 오류 - goodsVariantId: {}", goodsVariantId, e);
			return null;
		}
	}

	/**
	 * 현재 예약 가능 좌석 수 조회
	 */
	public Integer getCurrentScheduleCapacity(UUID scheduleId) {
		try {
			String sql = """
				SELECT remaining_capacity
				FROM p_popup_schedules
				WHERE schedule_id = ?
				AND deleted_at IS NULL
				AND is_active = true
				""";

			List<Integer> results = jdbcTemplate.query(sql,
				(rs, rowNum) -> rs.getInt("remaining_capacity"),
				scheduleId);

			return results.isEmpty() ? null : results.get(0);

		} catch (Exception e) {
			log.error("❌ 스케줄 잔여 좌석 조회 중 오류 - scheduleId: {}", scheduleId, e);
			return null;
		}
	}

	/**
	 * 예약 상품 재고 차감 (스케줄별 좌석 수)
	 */
	private void deductReservationInventory(UUID scheduleId, Integer qty) {
		try {
			// 현재 잔여 좌석 확인
			Integer currentCapacity = getCurrentScheduleCapacity(scheduleId);
			if (currentCapacity == null) {
				log.error("❌ 스케줄을 찾을 수 없음 - scheduleId: {}", scheduleId);
				throw new IllegalStateException("스케줄을 찾을 수 없습니다: " + scheduleId);
			}

			if (currentCapacity < qty) {
				log.error("❌ 예약 좌석 부족 - scheduleId: {}, 현재좌석: {}, 요청수량: {}",
					scheduleId, currentCapacity, qty);
				throw new IllegalStateException(
					String.format("예약 가능한 좌석이 부족합니다. 현재 잔여 좌석: %d석, 요청 수량: %d석", currentCapacity, qty));
			}

			// 좌석 차감
			String sql = """
				UPDATE p_popup_schedules
				SET remaining_capacity = remaining_capacity - ?,
					updated_at = CURRENT_TIMESTAMP
				WHERE schedule_id = ?
				AND remaining_capacity >= ?
				AND deleted_at IS NULL
				""";

			int rowsUpdated = jdbcTemplate.update(sql, qty, scheduleId, qty);

			if (rowsUpdated == 0) {
				log.error("❌ 예약 재고 차감 실패 - 동시성 문제 또는 스케줄 상태 변경: scheduleId={}, qty={}",
					scheduleId, qty);
				throw new IllegalStateException("예약 좌석 차감에 실패했습니다. 다시 시도해주세요.");
			}

			Integer remainingCapacity = currentCapacity - qty;
			log.info("📅 예약 재고 차감 완료 - scheduleId: {}, 차감수량: {}석, 잔여좌석: {}석",
				scheduleId, qty, remainingCapacity);

			// 잔여 좌석 부족 경고
			if (remainingCapacity <= 3) {
				log.warn("⚠️ 예약 좌석 부족 주의 - scheduleId: {}, 잔여좌석: {}석", scheduleId, remainingCapacity);
			}

		} catch (Exception e) {
			log.error("❌ 예약 재고 차감 중 오류 - scheduleId: {}, qty: {}", scheduleId, qty, e);
			throw new RuntimeException("예약 재고 차감 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * 일반 상품 재고 차감
	 */
	private void deductGoodsInventory(UUID goodsVariantId, Integer qty) {
		try {
			// 현재 재고 확인
			Integer currentStock = getCurrentGoodsStock(goodsVariantId);
			if (currentStock == null) {
				log.error("❌ 상품을 찾을 수 없음 - goodsVariantId: {}", goodsVariantId);
				throw new IllegalStateException("상품을 찾을 수 없습니다: " + goodsVariantId);
			}

			if (currentStock < qty) {
				log.error("❌ 상품 재고 부족 - goodsVariantId: {}, 현재재고: {}, 요청수량: {}",
					goodsVariantId, currentStock, qty);
				throw new IllegalStateException(
					String.format("상품 재고가 부족합니다. 현재 재고: %d개, 요청 수량: %d개", currentStock, qty));
			}

			// 재고 차감
			String sql = """
				UPDATE p_goods_variants
				SET stock = stock - ?,
					updated_at = CURRENT_TIMESTAMP
				WHERE goods_id = ?
				AND stock >= ?
				AND deleted_at IS NULL
				""";

			int rowsUpdated = jdbcTemplate.update(sql, qty, goodsVariantId, qty);

			if (rowsUpdated == 0) {
				log.error("❌ 상품 재고 차감 실패 - 동시성 문제 또는 상품 상태 변경: goodsVariantId={}, qty={}",
					goodsVariantId, qty);
				throw new IllegalStateException("상품 재고 차감에 실패했습니다. 다시 시도해주세요.");
			}

			Integer remainingStock = currentStock - qty;
			log.info("📦 상품 재고 차감 완료 - goodsVariantId: {}, 차감수량: {}, 잔여재고: {}개",
				goodsVariantId, qty, remainingStock);

			// 재고 부족 경고
			if (remainingStock <= 5) {
				log.warn("⚠️ 상품 재고 부족 주의 - goodsVariantId: {}, 잔여재고: {}개", goodsVariantId, remainingStock);
			}

		} catch (Exception e) {
			log.error("❌ 상품 재고 차감 중 오류 - goodsVariantId: {}, qty: {}", goodsVariantId, qty, e);
			throw new RuntimeException("상품 재고 차감 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * 재고 복원 (결제 실패 시 사용)
	 */
	@Transactional
	public void restoreInventoryForOrder(UUID orderId, List<OrderItem> orderItems) {
		log.info("🔄 재고 복원 시작 - 주문ID: {}, 항목 수: {}", orderId, orderItems.size());

		for (OrderItem item : orderItems) {
			if (item.getSessionOptionId() != null) {
				restoreReservationInventory(item.getSessionOptionId(), item.getQty());
			} else if (item.getGoodsVariantId() != null) {
				restoreGoodsInventory(item.getGoodsVariantId(), item.getQty());
			}
		}

		log.info("✅ 재고 복원 완료 - 주문ID: {}", orderId);
	}

	private void restoreReservationInventory(UUID scheduleId, Integer qty) {
		try {
			String sql = """
				UPDATE p_popup_schedules
				SET remaining_capacity = remaining_capacity + ?
				WHERE schedule_id = ?
				AND deleted_at IS NULL
				""";

			jdbcTemplate.update(sql, qty, scheduleId);
			log.info("📅 예약 재고 복원 완료 - scheduleId: {}, 복원수량: {}", scheduleId, qty);

		} catch (Exception e) {
			log.error("❌ 예약 재고 복원 중 오류 - scheduleId: {}, qty: {}", scheduleId, qty, e);
		}
	}

	private void restoreGoodsInventory(UUID goodsVariantId, Integer qty) {
		try {
			String sql = """
				UPDATE p_goods_variants
				SET stock = stock + ?
				WHERE goods_id = ?
				AND deleted_at IS NULL
				""";

			jdbcTemplate.update(sql, qty, goodsVariantId);
			log.info("📦 상품 재고 복원 완료 - goodsVariantId: {}, 복원수량: {}", goodsVariantId, qty);

		} catch (Exception e) {
			log.error("❌ 상품 재고 복원 중 오류 - goodsVariantId: {}, qty: {}", goodsVariantId, qty, e);
		}
	}
}