package com.popcorn.demo.domain.store.service;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 스토어 애플리케이션 서비스
 * - 유스케이스 조정 및 트랜잭션 관리
 * - 비동기 검증 처리
 * - 인프라스트럭처 의존성 관리
 */
@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private static final int MAX_STORES_PER_OWNER = 10;
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final StoreDomainService storeDomainService;
    private final StoreRepository storeRepository;
    private final Executor validationExecutor;
    private final MeterRegistry meterRegistry;

    public StoreService(
            StoreDomainService storeDomainService,
            StoreRepository storeRepository,
            @Qualifier("storeValidationTaskExecutor") Executor validationExecutor,
            MeterRegistry meterRegistry) {
        this.storeDomainService = storeDomainService;
        this.storeRepository = storeRepository;
        this.validationExecutor = validationExecutor;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 스토어 생성 메인 메서드
     * - 비동기 검증 처리
     * - 트랜잭션 관리
     * - 성능 모니터링
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = {Exception.class},
        timeout = 30
    )
    public StoreCreatedDto createStore(Long ownerId, CreateStoreRequest request, String idempotencyKey) {
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();
        
        log.info("[STORE_CREATE_START] correlationId={}, ownerId={}, storeName={}", 
                 correlationId, ownerId, request.getName());
        
        Timer timer = Timer.builder("store.creation.duration")
            .description("Store creation duration")
            .tag("owner", String.valueOf(ownerId))
            .register(meterRegistry);
        
        long startNanos = System.nanoTime();
        
        try {
            // 1단계: 동기 기본 검증 (빠른 실패)
            storeDomainService.validateStoreCreation(ownerId, request.getName());
            
            // 2단계: 비동기 복합 검증 실행 및 대기
            ValidationResult validationResult = validateStoreCreationAsync(request, ownerId)
                .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
            if (!validationResult.isSuccess()) {
                throw validationResult.getException();
            }
            
            // 3단계: 스토어 생성 및 저장
            StoreCreatedDto result = performStoreCreation(ownerId, request, correlationId);
            
            // 4단계: 비즈니스 메트릭 수집
            recordBusinessMetrics(result);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[STORE_CREATE_SUCCESS] correlationId={}, storeId={}, duration={}ms", 
                     correlationId, result.getId(), duration);
            
            return result;
            
        } catch (StoreException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_BUSINESS_ERROR] correlationId={}, ownerId={}, error={}, duration={}ms", 
                      correlationId, ownerId, e.getMessage(), duration, e);
            incrementErrorCounter("business_error", e.getClass().getSimpleName());
            throw e;
            
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_TIMEOUT] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration);
            incrementErrorCounter("timeout", "ValidationTimeout");
            throw StoreException.validationTimeout();
            
        } catch (ExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            Throwable cause = e.getCause();
            if (cause instanceof StoreException) {
                log.error("[STORE_CREATE_ASYNC_ERROR] correlationId={}, ownerId={}, error={}, duration={}ms", 
                          correlationId, ownerId, cause.getMessage(), duration, cause);
                incrementErrorCounter("async_validation", cause.getClass().getSimpleName());
                throw (StoreException) cause;
            }
            log.error("[STORE_CREATE_EXECUTION_ERROR] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration, e);
            incrementErrorCounter("execution", "ExecutionException");
            throw StoreException.validationFailed("Validation failed", cause);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_INTERRUPTED] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration);
            incrementErrorCounter("interrupted", "InterruptedException");
            throw StoreException.validationInterrupted();
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_SYSTEM_ERROR] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration, e);
            incrementErrorCounter("system", e.getClass().getSimpleName());
            throw StoreException.validationFailed("스토어 생성 중 시스템 오류가 발생했습니다.", e);
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            timer.record(durationNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 비동기 검증 로직 - 병렬 처리로 성능 최적화
     */
    private CompletableFuture<ValidationResult> validateStoreCreationAsync(CreateStoreRequest request, Long ownerId) {
        // 병렬 비동기 검증 실행
        CompletableFuture<Boolean> duplicateCheck = CompletableFuture.supplyAsync(() -> {
            Optional<Store> existingStore = storeRepository.findByName(request.getName());
            return storeDomainService.isDuplicateStore(existingStore, null);
        }, validationExecutor);
        
        CompletableFuture<Boolean> permissionCheck = CompletableFuture.supplyAsync(() -> {
            // 외부 API 호출 시뮬레이션 (실제로는 UserServiceClient 사용)
            return validateOwnerPermissionExternal(ownerId);
        }, validationExecutor);
        
        CompletableFuture<Integer> storeCountCheck = CompletableFuture.supplyAsync(() -> {
            return (int) storeRepository.countByOwnerId(ownerId);
        }, validationExecutor);
        
        // 모든 비동기 작업 완료 대기 및 결과 조합
        return CompletableFuture.allOf(duplicateCheck, permissionCheck, storeCountCheck)
            .thenApply(v -> {
                try {
                    boolean isDuplicate = duplicateCheck.get();
                    boolean hasPermission = permissionCheck.get();
                    int storeCount = storeCountCheck.get();
                    
                    // Domain Service로 비즈니스 검증 위임
                    validateBusinessRules(request.getName(), ownerId, isDuplicate, hasPermission, storeCount);
                    
                    return ValidationResult.success();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            })
            .exceptionally(throwable -> {
                log.error("Async validation failed for ownerId={}, storeName={}", ownerId, request.getName(), throwable);
                if (throwable.getCause() instanceof StoreException) {
                    return ValidationResult.failure((StoreException) throwable.getCause());
                }
                return ValidationResult.failure(StoreException.validationFailed("비동기 검증 실패", throwable));
            });
    }

    /**
     * 비즈니스 규칙 검증 (Domain Service 활용)
     */
    private void validateBusinessRules(String name, Long ownerId, boolean isDuplicate, boolean hasPermission, int storeCount) {
        if (isDuplicate) {
            throw StoreException.duplicateStoreName(name);
        }
        
        if (!hasPermission) {
            throw StoreException.ownerNotAuthorized(ownerId);
        }

    }

    /**
     * 외부 권한 검증 (실제로는 외부 서비스 호출)
     */
    private boolean validateOwnerPermissionExternal(Long ownerId) {
        // 실제 구현에서는 UserServiceClient.validateUser(ownerId) 호출
        // 현재는 시뮬레이션
        return ownerId != null && ownerId > 0;
    }

    /**
     * 실제 스토어 생성 로직
     */
    private StoreCreatedDto performStoreCreation(Long ownerId, CreateStoreRequest request, String correlationId) {
        log.debug("[STORE_CREATE_ENTITY] correlationId={}, creating store entity", correlationId);
        
        Store store = Store.builder()
                .name(request.getName())
                .ownerId(ownerId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(ownerId)
                .updatedBy(ownerId)
                .build();

        Store savedStore = storeRepository.save(store);
        
        log.debug("[STORE_CREATE_SAVED] correlationId={}, storeId={}", correlationId, savedStore.getId());

        return StoreCreatedDto.builder()
                .id(savedStore.getId())
                .name(savedStore.getName())
                .ownerId(savedStore.getOwnerId())
                .publishStatus(savedStore.getPublishStatus())
                .createdAt(savedStore.getCreatedAt())
                .createdBy(savedStore.getCreatedBy())
                .build();
    }

    /**
     * 비즈니스 메트릭 수집
     */
    private void recordBusinessMetrics(StoreCreatedDto createdStore) {
        // 스토어 생성 카운터
        Counter.builder("store.created.total")
            .description("Total stores created")
            .tag("status", createdStore.getPublishStatus().name())
            .tag("owner", String.valueOf(createdStore.getOwnerId()))
            .register(meterRegistry)
            .increment();
    }

    /**
     * 에러 카운터 증가
     */
    private void incrementErrorCounter(String errorType, String errorClass) {
        Counter.builder("store.creation.errors")
            .description("Store creation errors")
            .tag("type", errorType)
            .tag("class", errorClass)
            .register(meterRegistry)
            .increment();
    }

    /**
     * 검증 결과를 담는 내부 클래스
     */
    private static class ValidationResult {
        private final boolean success;
        private final StoreException exception;

        private ValidationResult(boolean success, StoreException exception) {
            this.success = success;
            this.exception = exception;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(StoreException exception) {
            return new ValidationResult(false, exception);
        }

        public boolean isSuccess() {
            return success;
        }

        public StoreException getException() {
            return exception;
        }
    }
}