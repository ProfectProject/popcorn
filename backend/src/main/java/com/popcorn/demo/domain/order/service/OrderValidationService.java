package com.popcorn.demo.domain.order.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

	/**
	 * 주문 검증 처리 (비동기 최적화)
	 * - 상품 재고 확인
	 * - 고객 신용도 확인
	 * - 프로모션 유효성 확인
	 */
	public boolean validateOrderAsync(Long userId, UUID productId, Integer qty) {
		try {
			// 병렬 검증으로 성능 최적화
			CompletableFuture<Boolean> stockValidation = CompletableFuture
					.supplyAsync(() -> validateStock(qty));

			CompletableFuture<Boolean> userValidation = CompletableFuture
					.supplyAsync(() -> validateCustomer(userId));

			CompletableFuture<Boolean> productValidation = CompletableFuture
					.supplyAsync(() -> validateProduct(productId));

			// 모든 검증 결과 조합
			boolean result = stockValidation.get() && userValidation.get() && productValidation.get();

			log.info("📋 주문 검증 완료 - 사용자: {}, 상품: {}, 결과: {}", userId, productId, result);
			return result;

		} catch (Exception e) {
			log.error("❌ 주문 검증 중 오류 발생 - 사용자: {}, 상품: {}", userId, productId, e);
			return false;
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
	 * 고객 검증 (최적화됨)
	 */
	private boolean validateCustomer(Long userId) {
		if (userId == null || userId <= 0) {
			log.warn("⚠️ 잘못된 사용자 ID: {}", userId);
			return false;
		}

		// TODO: 실제 사용자 시스템과 연동
		boolean isValidCustomer = userId > 0;
		log.debug("👤 고객 확인 - 사용자ID: {}, 유효성: {}", userId, isValidCustomer);

		return isValidCustomer;
	}

	/**
	 * 상품 검증 (최적화됨)
	 */
	private boolean validateProduct(UUID productId) {
		if (productId == null) {
			log.warn("⚠️ 잘못된 상품 ID: {}", productId);
			return false;
		}

		// TODO: 실제 상품 시스템과 연동
		boolean isValidProduct = true; // 임시 로직
		log.debug("📱 상품 확인 - 상품ID: {}, 유효성: {}", productId, isValidProduct);

		return isValidProduct;
	}
}