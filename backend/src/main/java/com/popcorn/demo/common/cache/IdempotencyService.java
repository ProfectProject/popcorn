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

import lombok.RequiredArgsConstructor;

/**
 * 향상된 멱등성 처리 서비스
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
public class IdempotencyService {

	private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

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
			.recordStats()
			.build();

	// 진행 중인 요청 추적: 동시 요청 처리를 위한 상태 관리
	private final Map<String, RequestState> inProgressRequests = new ConcurrentHashMap<>();

	/**
	 * 멱등성 키 기반 요청 처리
	 *
	 * @param idempotencyKey 멱등성 키
	 * @param operation 실제 비즈니스 로직
	 * @param responseType 응답 타입 클래스
	 * @return 캐싱된 응답 또는 새로운 실행 결과
	 */
	public <T> IdempotencyResult<T> processRequest(
			String idempotencyKey,
			IdempotentOperation<T> operation,
			Class<T> responseType) {

		if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
			// 멱등성 키가 없으면 직접 실행
			try {
				T result = operation.execute();
				metrics.recordDirectExecution();
				return IdempotencyResult.newExecution(result);
			} catch (Exception e) {
				metrics.recordOperationError();
				throw new IdempotencyException("작업 실행 중 오류 발생", e);
			}
		}

		String normalizedKey = normalizeKey(idempotencyKey);

		// 1. 완료된 요청 확인 (캐시된 응답 반환)
		IdempotencyRecord cachedRecord = responseCache.getIfPresent(normalizedKey);
		if (cachedRecord != null) {
			log.debug("🎯 멱등성 히트 - 키: {}", normalizedKey);
			metrics.recordCacheHit();

			try {
				T cachedResponse = objectMapper.readValue(cachedRecord.getResponseData(), responseType);
				return IdempotencyResult.cachedExecution(cachedResponse, cachedRecord.getCompletedAt());
			} catch (JsonProcessingException e) {
				log.error("❌ 캐시된 응답 역직렬화 실패 - 키: {}", normalizedKey, e);
				// 캐시 무효화 후 재실행
				responseCache.invalidate(normalizedKey);
			}
		}

		// 2. 진행 중인 요청 확인 (동시성 제어)
		RequestState existingState = inProgressRequests.putIfAbsent(normalizedKey,
				new RequestState(LocalDateTime.now()));

		if (existingState != null) {
			log.warn("⏳ 동일한 요청이 이미 처리 중 - 키: {}, 시작시간: {}",
					normalizedKey, existingState.getStartTime());
			metrics.recordConcurrentRequest();
			throw new IdempotencyException("동일한 요청이 이미 처리되고 있습니다", normalizedKey);
		}

		try {
			// 3. 새로운 요청 실행
			log.info("🚀 새로운 멱등성 요청 시작 - 키: {}", normalizedKey);
			metrics.recordNewRequest();

			T result = operation.execute();

			// 4. 성공 시 응답 캐싱
			try {
				String responseJson = objectMapper.writeValueAsString(result);
				IdempotencyRecord record = new IdempotencyRecord(
						normalizedKey,
						responseJson,
						LocalDateTime.now()
				);
				responseCache.put(normalizedKey, record);

				log.info("✅ 멱등성 요청 완료 및 캐싱 - 키: {}", normalizedKey);
				metrics.recordSuccessfulExecution();

				return IdempotencyResult.newExecution(result);

			} catch (JsonProcessingException e) {
				log.error("❌ 응답 직렬화 실패 - 키: {}", normalizedKey, e);
				// 직렬화 실패해도 결과는 반환
				metrics.recordSerializationError();
				return IdempotencyResult.newExecution(result);
			}

		} catch (Exception e) {
			log.error("❌ 멱등성 요청 실행 실패 - 키: {}", normalizedKey, e);
			metrics.recordOperationError();
			throw new IdempotencyException("요청 실행 중 오류가 발생했습니다", e);

		} finally {
			// 5. 진행 중 요청 상태 정리
			inProgressRequests.remove(normalizedKey);
			log.debug("🧹 진행 중 요청 상태 정리 - 키: {}", normalizedKey);
		}
	}

	/**
	 * 캐시 통계 조회
	 */
	public IdempotencyCacheStats getCacheStats() {
		com.github.benmanes.caffeine.cache.stats.CacheStats stats = responseCache.stats();

		return IdempotencyCacheStats.builder()
				.hitCount(stats.hitCount())
				.missCount(stats.missCount())
				.hitRate(stats.hitRate())
				.totalLoadTime(stats.totalLoadTime())
				.evictionCount(stats.evictionCount())
				.cacheSize(responseCache.estimatedSize())
				.inProgressRequestCount(inProgressRequests.size())
				.newRequestCount(metrics.getNewRequestCount())
				.cacheHitCount(metrics.getCacheHitCount())
				.concurrentRequestCount(metrics.getConcurrentRequestCount())
				.operationErrorCount(metrics.getOperationErrorCount())
				.build();
	}

	/**
	 * 캐시 정리 (관리자용)
	 */
	public void clearCache() {
		responseCache.invalidateAll();
		inProgressRequests.clear();
		log.info("🧹 멱등성 캐시 전체 정리 완료");
		metrics.recordCacheCleared();
	}

	/**
	 * 특정 키의 캐시 무효화
	 */
	public void invalidateKey(String idempotencyKey) {
		String normalizedKey = normalizeKey(idempotencyKey);
		responseCache.invalidate(normalizedKey);
		inProgressRequests.remove(normalizedKey);
		log.info("🗑️ 멱등성 키 무효화 - 키: {}", normalizedKey);
		metrics.recordKeyInvalidation();
	}

	// ================ 내부 헬퍼 메서드들 ================

	private String normalizeKey(String key) {
		if (key == null) {
			return null;
		}
		return key.trim().toLowerCase();
	}

	// ================ 내부 클래스들 ================

	/**
	 * 멱등성 레코드: 캐싱된 응답 정보
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
	 * 진행 중인 요청 상태
	 */
	private static class RequestState {
		private final LocalDateTime startTime;

		public RequestState(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getStartTime() { return startTime; }
	}

	/**
	 * 멱등성 메트릭스 수집기
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
	}

	/**
	 * 멱등성 처리 전용 예외
	 */
	public static class IdempotencyException extends RuntimeException {
		private final String idempotencyKey;

		public IdempotencyException(String message, String idempotencyKey) {
			super(message);
			this.idempotencyKey = idempotencyKey;
		}

		public IdempotencyException(String message, Throwable cause) {
			super(message, cause);
			this.idempotencyKey = null;
		}

		public String getIdempotencyKey() { return idempotencyKey; }
	}

	/**
	 * 멱등성 처리 결과
	 */
	public static class IdempotencyResult<T> {
		private final T result;
		private final boolean isCached;
		private final LocalDateTime executedAt;

		private IdempotencyResult(T result, boolean isCached, LocalDateTime executedAt) {
			this.result = result;
			this.isCached = isCached;
			this.executedAt = executedAt;
		}

		public static <T> IdempotencyResult<T> newExecution(T result) {
			return new IdempotencyResult<>(result, false, LocalDateTime.now());
		}

		public static <T> IdempotencyResult<T> cachedExecution(T result, LocalDateTime originalExecutionTime) {
			return new IdempotencyResult<>(result, true, originalExecutionTime);
		}

		public T getResult() { return result; }
		public boolean isCached() { return isCached; }
		public LocalDateTime getExecutedAt() { return executedAt; }
	}

	/**
	 * 멱등성 작업 인터페이스
	 */
	@FunctionalInterface
	public interface IdempotentOperation<T> {
		T execute() throws Exception;
	}
}