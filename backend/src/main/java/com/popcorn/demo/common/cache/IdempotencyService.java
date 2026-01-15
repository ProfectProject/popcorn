package com.popcorn.demo.common.cache;

import java.time.LocalDateTime;

/**
 * 멱등성 처리 서비스 인터페이스
 *
 * 다양한 멱등성 구현 방식을 지원하기 위한 인터페이스
 * - 캐시 기반 구현 (Caffeine, Redis 등)
 * - 데이터베이스 기반 구현
 * - 메모리 기반 구현
 * - 하이브리드 구현
 */
public interface IdempotencyService {

    /**
     * 멱등성을 보장하며 요청을 처리합니다
     *
     * @param idempotencyKey 멱등성 키
     * @param operation 실행할 작업
     * @param responseType 응답 타입 클래스
     * @param <T> 응답 타입
     * @return 멱등성 처리 결과
     */
    <T> IdempotencyResult<T> processRequest(
        String idempotencyKey,
        IdempotentOperation<T> operation,
        Class<T> responseType
    );

    /**
     * 캐시 통계 정보를 조회합니다
     *
     * @return 캐시 통계
     */
    IdempotencyCacheStats getCacheStats();

    /**
     * 전체 캐시를 초기화합니다
     */
    void clearCache();

    /**
     * 특정 키를 캐시에서 무효화합니다
     *
     * @param idempotencyKey 무효화할 멱등성 키
     */
    void invalidateKey(String idempotencyKey);

    /**
     * 멱등성 처리 결과
     */
    interface IdempotencyResult<T> {
        T getResult();
        boolean isFromCache();
        LocalDateTime getOriginalExecutionTime();

        static <T> IdempotencyResult<T> newExecution(T result) {
            return new DefaultIdempotencyResult<>(result, false, LocalDateTime.now());
        }

        static <T> IdempotencyResult<T> cachedExecution(T result, LocalDateTime originalExecutionTime) {
            return new DefaultIdempotencyResult<>(result, true, originalExecutionTime);
        }
    }

    /**
     * 기본 멱등성 결과 구현체
     */
    class DefaultIdempotencyResult<T> implements IdempotencyResult<T> {
        private final T result;
        private final boolean fromCache;
        private final LocalDateTime originalExecutionTime;

        public DefaultIdempotencyResult(T result, boolean fromCache, LocalDateTime originalExecutionTime) {
            this.result = result;
            this.fromCache = fromCache;
            this.originalExecutionTime = originalExecutionTime;
        }

        @Override
        public T getResult() {
            return result;
        }

        @Override
        public boolean isFromCache() {
            return fromCache;
        }

        @Override
        public LocalDateTime getOriginalExecutionTime() {
            return originalExecutionTime;
        }
    }

    /**
     * 멱등성 처리 예외
     */
    class IdempotencyException extends RuntimeException {
        private final String idempotencyKey;

        public IdempotencyException(String message, String idempotencyKey) {
            super(message);
            this.idempotencyKey = idempotencyKey;
        }

        public IdempotencyException(String message, Throwable cause) {
            super(message, cause);
            this.idempotencyKey = null;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }
    }
}