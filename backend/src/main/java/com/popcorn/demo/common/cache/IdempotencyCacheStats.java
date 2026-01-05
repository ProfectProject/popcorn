package com.popcorn.demo.common.cache;

import lombok.Builder;
import lombok.Getter;

/**
 * 멱등성 캐시 통계 정보
 *
 * 캐시 성능 모니터링 및 분석을 위한 메트릭스
 */
@Getter
@Builder
public class IdempotencyCacheStats {

	// Caffeine 캐시 기본 통계
	private final long hitCount;              // 캐시 히트 횟수
	private final long missCount;             // 캐시 미스 횟수
	private final double hitRate;             // 캐시 히트율 (0.0 ~ 1.0)
	private final long totalLoadTime;        // 총 로드 시간 (나노초)
	private final long evictionCount;         // 캐시 제거 횟수
	private final long cacheSize;             // 현재 캐시 크기

	// 멱등성 서비스 전용 통계
	private final int inProgressRequestCount;  // 현재 진행 중인 요청 수
	private final long newRequestCount;        // 새로운 요청 수 (전체)
	private final long cacheHitCount;          // 캐시에서 응답한 요청 수
	private final long concurrentRequestCount; // 동시 요청으로 거부된 수
	private final long operationErrorCount;    // 작업 실행 오류 수

	/**
	 * 캐시 효율성 계산
	 */
	public double getCacheEfficiency() {
		long totalRequests = hitCount + missCount;
		return totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;
	}

	/**
	 * 평균 로드 시간 (밀리초)
	 */
	public double getAverageLoadTimeMs() {
		return missCount > 0 ? totalLoadTime / 1_000_000.0 / missCount : 0.0;
	}

	/**
	 * 동시성 문제 비율
	 */
	public double getConcurrencyIssueRate() {
		long totalRequests = newRequestCount + cacheHitCount + concurrentRequestCount;
		return totalRequests > 0 ? (double) concurrentRequestCount / totalRequests : 0.0;
	}

	/**
	 * 오류 발생율
	 */
	public double getErrorRate() {
		long totalRequests = newRequestCount + cacheHitCount + concurrentRequestCount;
		return totalRequests > 0 ? (double) operationErrorCount / totalRequests : 0.0;
	}

	/**
	 * 통계 정보를 읽기 쉬운 문자열로 변환
	 */
	@Override
	public String toString() {
		return String.format("""
				💾 멱등성 캐시 통계
				━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
				🎯 캐시 성능
				   - 히트율: %.2f%% (%d hits / %d total)
				   - 캐시 크기: %d entries
				   - 평균 로드 시간: %.2f ms
				   - 제거된 항목: %d

				🔄 요청 처리
				   - 새로운 요청: %d
				   - 캐시된 응답: %d
				   - 진행 중: %d
				   - 동시 거부: %d (%.2f%%)

				❌ 오류 통계
				   - 실행 오류: %d (%.2f%%)
				━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
				""",
				hitRate * 100, hitCount, hitCount + missCount,
				cacheSize,
				getAverageLoadTimeMs(),
				evictionCount,
				newRequestCount,
				cacheHitCount,
				inProgressRequestCount,
				concurrentRequestCount, getConcurrencyIssueRate() * 100,
				operationErrorCount, getErrorRate() * 100
		);
	}
}