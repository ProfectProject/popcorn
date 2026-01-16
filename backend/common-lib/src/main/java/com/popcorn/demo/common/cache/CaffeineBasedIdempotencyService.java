package com.popcorn.demo.common.cache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Caffeine 캐시 기반 멱등성 처리 서비스 구현체
 *
 * 기능:
 * - 요청 상태 추적 (IN_PROGRESS, COMPLETED)
 * - 응답 캐싱 및 반환
 * - 동시성 제어 (Race Condition 해결)
 * - 메트릭스 수집
 * - 설정 가능한 캐시 정책
 */
@Service
@RequiredArgsConstructor
public class CaffeineBasedIdempotencyService implements IdempotencyService {

	private static final Logger log = LoggerFactory.getLogger(CaffeineBasedIdempotencyService.class);

	private final ObjectMapper objectMapper;

	// 메트릭스 수집 (먼저 선언)
	private final IdempotencyMetrics metrics = new IdempotencyMetrics();

	// 응답 캐시: 완료된 요청의 실제 응답을 저장
	private final Cache<String, IdempotencyRecord> responseCache = Caffeine.newBuilder()
			.maximumSize(10_000)
			.expireAfterWrite(Duration.ofMinutes(30)) // 응답은 30분 보관
			.removalListener((RemovalListener<String, IdempotencyRecord>) (key, value, cause) -> {
				log.debug("💾 멱등성 레코드 만료 - 키: {}, 원인: {}", key, cause);
				metrics.recordCacheEviction();
			})
			.build();

	// 진행 중 요청 추적: 동시 요청 감지용
	private final Map<String, ProgressRecord> inProgressRequests = new ConcurrentHashMap<>();

	@Override
	public <T> IdempotencyResult<T> processRequest(
			String idempotencyKey,
			IdempotentOperation<T> operation,
			Class<T> responseType) {

		log.debug("🔄 멱등성 요청 처리 시작 - 키: {}", idempotencyKey);

		// null key인 경우 캐시를 사용하지 않고 바로 실행
		if (idempotencyKey == null) {
			log.debug("⚠️ 멱등성 키가 null이므로 캐시 없이 직접 실행");
			try {
				T result = operation.execute();
				return IdempotencyResult.newExecution(result);
			} catch (Exception e) {
				log.error("❌ 요청 처리 실패 - 키: null, 오류: {}", e.getMessage(), e);
				throw new IdempotencyException("작업 실행 중 오류 발생", e);
			}
		}

		// 1. 이미 완료된 요청인지 확인
		IdempotencyRecord cachedRecord = responseCache.getIfPresent(idempotencyKey);
		if (cachedRecord != null) {
			log.debug("✅ 캐시에서 응답 반환 - 키: {}", idempotencyKey);
			metrics.recordCacheHit();

			T cachedResult = deserializeResponse(cachedRecord.getResponseData(), responseType);
			return IdempotencyResult.cachedExecution(cachedResult, cachedRecord.getCompletedAt());
		}

		// 2. 동시 요청 체크 및 처리
		ProgressRecord existingProgress = inProgressRequests.get(idempotencyKey);
		if (existingProgress != null) {
			log.warn("🔄 동시 요청 감지 - 키: {} (시작 시간: {})", idempotencyKey, existingProgress.getStartTime());
			metrics.recordConcurrentRequest();

			// 진행 중인 요청이 있으면 대기 후 재시도하거나, 예외를 던짐
			throw new IdempotencyException(
					String.format("동시에 처리 중인 요청이 있습니다. 키: %s", idempotencyKey),
					idempotencyKey);
		}

		// 3. 새로운 요청 처리
		ProgressRecord progressRecord = new ProgressRecord(idempotencyKey, LocalDateTime.now());
		inProgressRequests.put(idempotencyKey, progressRecord);

		try {
			log.debug("🚀 새 요청 실행 시작 - 키: {}", idempotencyKey);
			metrics.recordNewRequest();

			// 실제 작업 실행
			T result = operation.execute();

			// 4. 성공 결과 캐싱 (직렬화 실패 시에도 결과는 반환)
			try {
				String serializedResult = serializeResponse(result);
				IdempotencyRecord record = new IdempotencyRecord(
						idempotencyKey,
						serializedResult,
						LocalDateTime.now()
				);

				responseCache.put(idempotencyKey, record);
				log.debug("✅ 요청 완료 및 캐싱 - 키: {}", idempotencyKey);
			} catch (Exception e) {
				log.warn("⚠️ 결과 캐싱 실패하지만 요청은 성공 처리 - 키: {}, 오류: {}",
					idempotencyKey, e.getMessage());
			}

			metrics.recordSuccessfulExecution();
			return IdempotencyResult.newExecution(result);

		} catch (Exception e) {
			log.error("❌ 요청 처리 실패 - 키: {}, 오류: {}", idempotencyKey, e.getMessage());
			metrics.recordOperationError();
			throw new IdempotencyException("작업 실행 중 오류 발생", e);

		} finally {
			// 진행 상태 정리
			inProgressRequests.remove(idempotencyKey);
		}
	}

	@Override
	public IdempotencyCacheStats getCacheStats() {
		return CaffeineIdempotencyCacheStats.builder()
				.cacheSize(responseCache.estimatedSize())
				.hitCount(metrics.getCacheHitCount())
				.missCount(metrics.getNewRequestCount())
				.hitRate(calculateHitRate())
				.totalLoadTime(0L) // Caffeine 통계가 필요한 경우 별도 구현 필요
				.evictionCount(metrics.getCacheEvictionCount())
				.inProgressRequestCount(inProgressRequests.size())
				.newRequestCount(metrics.getNewRequestCount())
				.cacheHitCount(metrics.getCacheHitCount())
				.concurrentRequestCount(metrics.getConcurrentRequestCount())
				.operationErrorCount(metrics.getOperationErrorCount())
				.build();
	}

	@Override
	public void clearCache() {
		responseCache.invalidateAll();
		inProgressRequests.clear();
		log.info("🧹 멱등성 캐시 전체 초기화");
		metrics.recordCacheCleared();
	}

	@Override
	public void invalidateKey(String idempotencyKey) {
		responseCache.invalidate(idempotencyKey);
		inProgressRequests.remove(idempotencyKey);
		log.debug("🗑️ 멱등성 키 무효화 - 키: {}", idempotencyKey);
		metrics.recordKeyInvalidation();
	}

	// ================ Helper Methods ================

	private <T> String serializeResponse(T response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			metrics.recordSerializationError();
			throw new IdempotencyException("응답 직렬화 실패", e);
		}
	}

	private <T> T deserializeResponse(String responseData, Class<T> responseType) {
		try {
			return objectMapper.readValue(responseData, responseType);
		} catch (JsonProcessingException e) {
			metrics.recordSerializationError();
			throw new IdempotencyException("응답 역직렬화 실패", e);
		}
	}

	private double calculateHitRate() {
		long totalRequests = metrics.getNewRequestCount() + metrics.getCacheHitCount();
		if (totalRequests == 0) return 0.0;
		return (double) metrics.getCacheHitCount() / totalRequests;
	}

	// ================ 내부 클래스 ================

	/**
	 * 멱등성 레코드 (완료된 요청)
	 */
	private static class IdempotencyRecord {
		private final String key;
		private final String responseData;
		private final LocalDateTime completedAt;

		public IdempotencyRecord(String key, String responseData, LocalDateTime completedAt) {
			this.key = key;
			this.responseData = responseData;
			this.completedAt = completedAt;
		}

		public String getKey() { return key; }
		public String getResponseData() { return responseData; }
		public LocalDateTime getCompletedAt() { return completedAt; }
	}

	/**
	 * 진행 중 레코드 (처리 중인 요청)
	 */
	@Getter
	private static class ProgressRecord {
		private final String key;
		private final LocalDateTime startTime;

		public ProgressRecord(String key, LocalDateTime startTime) {
			this.key = key;
			this.startTime = startTime;
		}
	}

	/**
	 * 멱등성 메트릭스
	 */
	private static class IdempotencyMetrics {
		private final AtomicLong newRequestCount = new AtomicLong(0);
		private final AtomicLong cacheHitCount = new AtomicLong(0);
		private final AtomicLong concurrentRequestCount = new AtomicLong(0);
		private final AtomicLong operationErrorCount = new AtomicLong(0);
		private final AtomicLong serializationErrorCount = new AtomicLong(0);
		private final AtomicLong directExecutionCount = new AtomicLong(0);
		private final AtomicLong cacheEvictionCount = new AtomicLong(0);
		private final AtomicLong keyInvalidationCount = new AtomicLong(0);
		private final AtomicLong cacheClearCount = new AtomicLong(0);

		public void recordNewRequest() { newRequestCount.incrementAndGet(); }
		public void recordCacheHit() { cacheHitCount.incrementAndGet(); }
		public void recordConcurrentRequest() { concurrentRequestCount.incrementAndGet(); }
		public void recordOperationError() { operationErrorCount.incrementAndGet(); }
		public void recordSerializationError() { serializationErrorCount.incrementAndGet(); }
		public void recordDirectExecution() { directExecutionCount.incrementAndGet(); }
		public void recordCacheEviction() { cacheEvictionCount.incrementAndGet(); }
		public void recordKeyInvalidation() { keyInvalidationCount.incrementAndGet(); }
		public void recordCacheCleared() { cacheClearCount.incrementAndGet(); }
		public void recordSuccessfulExecution() { /* 성공적인 실행은 별도 카운터 불필요 */ }

		public long getNewRequestCount() { return newRequestCount.get(); }
		public long getCacheHitCount() { return cacheHitCount.get(); }
		public long getConcurrentRequestCount() { return concurrentRequestCount.get(); }
		public long getOperationErrorCount() { return operationErrorCount.get(); }
		public long getSerializationErrorCount() { return serializationErrorCount.get(); }
		public long getDirectExecutionCount() { return directExecutionCount.get(); }
		public long getCacheEvictionCount() { return cacheEvictionCount.get(); }
		public long getKeyInvalidationCount() { return keyInvalidationCount.get(); }
		public long getCacheClearCount() { return cacheClearCount.get(); }
	}
}