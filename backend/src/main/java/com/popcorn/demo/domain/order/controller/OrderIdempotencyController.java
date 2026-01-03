package com.popcorn.demo.domain.order.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.common.cache.IdempotencyCacheStats;
import com.popcorn.demo.common.dto.BaseResponse;

import lombok.RequiredArgsConstructor;

/**
 * 멱등성 캐시 관리 및 모니터링 컨트롤러
 *
 * 기능:
 * - 캐시 통계 조회
 * - 캐시 관리 (전체 삭제, 특정 키 삭제)
 * - 운영 모니터링 지원
 */
@RestController
@RequestMapping("/api/v1/orders/idempotency")
@RequiredArgsConstructor
public class OrderIdempotencyController {

	private static final Logger log = LoggerFactory.getLogger(OrderIdempotencyController.class);

	private final IdempotencyService idempotencyService;

	/**
	 * 멱등성 캐시 통계 조회
	 *
	 * 캐시 성능, 히트율, 오류율 등 모니터링 정보 제공
	 */
	@GetMapping("/stats")
	public ResponseEntity<BaseResponse<IdempotencyCacheStats>> getCacheStats() {
		log.info("📊 멱등성 캐시 통계 조회 요청");

		try {
			IdempotencyCacheStats stats = idempotencyService.getCacheStats();
			log.debug("📈 캐시 통계 - 히트율: {:.2f}%, 캐시크기: {}",
					stats.getHitRate() * 100, stats.getCacheSize());

			return ResponseEntity.ok(BaseResponse.success(stats));

		} catch (Exception e) {
			log.error("❌ 캐시 통계 조회 실패", e);
			throw new RuntimeException("캐시 통계 조회 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 캐시 통계 텍스트 형태로 조회 (운영자용)
	 *
	 * 가독성이 좋은 텍스트 형태로 통계 정보 제공
	 */
	@GetMapping("/stats/text")
	public ResponseEntity<String> getCacheStatsText() {
		log.info("📋 멱등성 캐시 통계 텍스트 조회 요청");

		try {
			IdempotencyCacheStats stats = idempotencyService.getCacheStats();
			String textStats = stats.toString();

			log.debug("📄 텍스트 통계 생성 완료");
			return ResponseEntity.ok(textStats);

		} catch (Exception e) {
			log.error("❌ 텍스트 통계 조회 실패", e);
			throw new RuntimeException("텍스트 통계 조회 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 전체 캐시 삭제 (관리자용)
	 *
	 * ⚠️ 주의: 모든 멱등성 캐시가 삭제됩니다
	 */
	@DeleteMapping("/cache")
	public ResponseEntity<BaseResponse<String>> clearAllCache() {
		log.warn("🧹 전체 멱등성 캐시 삭제 요청 - 관리자 작업");

		try {
			idempotencyService.clearCache();

			String message = "모든 멱등성 캐시가 성공적으로 삭제되었습니다";
			log.info("✅ {}", message);

			return ResponseEntity.ok(BaseResponse.success(message));

		} catch (Exception e) {
			log.error("❌ 전체 캐시 삭제 실패", e);
			throw new RuntimeException("캐시 삭제 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 특정 멱등성 키 캐시 삭제 (관리자용)
	 *
	 * @param idempotencyKey 삭제할 멱등성 키
	 */
	@DeleteMapping("/cache/{idempotencyKey}")
	public ResponseEntity<BaseResponse<String>> invalidateKey(@PathVariable String idempotencyKey) {
		log.warn("🗑️ 특정 멱등성 캐시 삭제 요청 - 키: {}", idempotencyKey);

		if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
			log.warn("❌ 잘못된 멱등성 키: {}", idempotencyKey);
			throw new IllegalArgumentException("올바른 멱등성 키를 입력해주세요");
		}

		try {
			idempotencyService.invalidateKey(idempotencyKey);

			String message = String.format("멱등성 키 '%s'의 캐시가 성공적으로 삭제되었습니다", idempotencyKey);
			log.info("✅ {}", message);

			return ResponseEntity.ok(BaseResponse.success(message));

		} catch (Exception e) {
			log.error("❌ 키 캐시 삭제 실패 - 키: {}", idempotencyKey, e);
			throw new RuntimeException("키 캐시 삭제 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 헬스체크 엔드포인트
	 *
	 * 멱등성 서비스 상태 확인
	 */
	@GetMapping("/health")
	public ResponseEntity<BaseResponse<String>> healthCheck() {
		log.debug("❤️ 멱등성 서비스 헬스체크");

		try {
			IdempotencyCacheStats stats = idempotencyService.getCacheStats();

			// 간단한 헬스체크 (캐시가 정상적으로 동작하는지 확인)
			boolean isHealthy = stats.getHitCount() >= 0 && stats.getMissCount() >= 0;

			if (isHealthy) {
				String message = String.format("멱등성 서비스 정상 - 캐시크기: %d, 히트율: %.2f%%",
						stats.getCacheSize(), stats.getHitRate() * 100);
				return ResponseEntity.ok(BaseResponse.success(message));
			} else {
				throw new RuntimeException("멱등성 서비스 상태 이상 감지");
			}

		} catch (Exception e) {
			log.error("❌ 멱등성 서비스 헬스체크 실패", e);
			throw new RuntimeException("멱등성 서비스가 정상적으로 동작하지 않습니다", e);
		}
	}
}