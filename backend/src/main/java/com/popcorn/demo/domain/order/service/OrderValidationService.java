package com.popcorn.demo.domain.order.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.order.exception.OrderNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 주문 검증 서비스
 *
 * CQRS 분리의 일환으로 검증 로직을 별도 서비스로 분리:
 * - 비동기 검증으로 성능 최적화
 * - 재사용 가능한 검증 로직
 * - 단일 책임 원칙 적용
 */
@Service
@RequiredArgsConstructor
public class OrderValidationService {

	private static final Logger log = LoggerFactory.getLogger(OrderValidationService.class);
	private final JdbcTemplate jdbcTemplate;

	/**
	 * 주문 검증 처리 (비동기 최적화)
	 * - 상품 재고 확인
	 * - 고객 신용도 확인
	 * - 프로모션 유효성 확인
	 */
	public boolean validateOrderAsync(Long userId, UUID popupId, Integer qty) {
		try {
			if (popupId == null) {
				log.warn("⚠️ 잘못된 상품 ID: null");
				return false;
			}

			// 병렬 검증으로 성능 최적화
			CompletableFuture<Boolean> stockValidation = CompletableFuture
					.supplyAsync(() -> validateStock(qty));

			CompletableFuture<Boolean> userValidation = CompletableFuture
					.supplyAsync(() -> validateCustomer(userId));

			CompletableFuture<Boolean> productValidation = CompletableFuture
					.supplyAsync(() -> validateProduct(popupId));

			// 모든 검증 결과 조합
			boolean result = stockValidation.get() && userValidation.get() && productValidation.get();

			log.info("📋 주문 검증 완료 - 사용자: {}, 상품: {}, 결과: {}", userId, popupId, result);
			return result;

		} catch (Exception e) {
			log.error("❌ 주문 검증 중 오류 발생 - 사용자: {}, 상품: {}", userId, popupId, e);
			return false;
		}
	}

	public UUID resolveStoreId(UUID popupId) {
		if (popupId == null) {
			throw OrderNotFoundException.productNotFound();
		}

		try {
			String sql = "SELECT store_id FROM p_popups WHERE popup_id = ? AND deleted_at IS NULL";
			UUID storeId = jdbcTemplate.queryForObject(sql, UUID.class, popupId);
			if (storeId == null) {
				throw OrderNotFoundException.storeNotFound();
			}
			return storeId;
		} catch (EmptyResultDataAccessException ex) {
			throw OrderNotFoundException.productNotFound();
		} catch (Exception ex) {
			log.error("❌ 스토어 조회 실패 - 상품ID: {}", popupId, ex);
			throw OrderNotFoundException.productNotFound();
		}
	}

	/**
	 * 재고 검증 (최적화됨)
	 */
	private boolean validateStock(Integer qty) {
		if (qty == null || qty <= 0) {
			log.warn("⚠️ 잘못된 수량: {}", qty);
			return false;
		}

		// TODO: 실제 재고 시스템과 연동
		boolean hasStock = qty <= 100; // 임시 로직
		log.debug("📦 재고 확인 - 요청수량: {}, 재고충분: {}", qty, hasStock);

		return hasStock;
	}

	/**
	 * 고객 검증 (데이터베이스 실제 확인)
	 */
	private boolean validateCustomer(Long userId) {
		if (userId == null || userId <= 0) {
			log.warn("⚠️ 잘못된 사용자 ID: {}", userId);
			return false;
		}

		try {
			// 실제 데이터베이스에서 사용자 존재 여부 확인
			String sql = "SELECT COUNT(*) FROM p_users WHERE user_id = ? AND is_active = TRUE";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
			boolean isValidCustomer = count != null && count > 0;

			log.debug("👤 고객 확인 - 사용자ID: {}, 유효성: {}", userId, isValidCustomer);

			if (!isValidCustomer) {
				log.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", userId);
			}

			return isValidCustomer;
		} catch (Exception e) {
			log.error("❌ 사용자 검증 중 데이터베이스 오류 - 사용자ID: {}", userId, e);
			return false;
		}
	}

	/**
	 * 상품 검증 (최적화됨)
	 */
	private boolean validateProduct(UUID popupId) {
		// TODO: 실제 상품 시스템과 연동
		boolean isValidProduct = true; // 임시 로직
		log.debug("📱 상품 확인 - 상품ID: {}, 유효성: {}", popupId, isValidProduct);

		return isValidProduct;
	}
}
