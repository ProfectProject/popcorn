package com.popcorn.demo.domain.store.service;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;
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
 * 스토어 서비스 (통합)
 * - 비즈니스 로직과 애플리케이션 로직을 모두 포함
 * - domain/order 구조를 참고한 Layered Architecture
 */
@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private static final int MAX_STORES_PER_OWNER = 10;
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;
    private static final int MAX_STORE_NAME_LENGTH = 100;
    private static final int MIN_STORE_NAME_LENGTH = 1;

    private final StoreRepository storeRepository;
    private final Executor validationExecutor;

    public StoreService(
            StoreRepository storeRepository,
            @Qualifier("storeValidationTaskExecutor") Executor validationExecutor) {
        this.storeRepository = storeRepository;
        this.validationExecutor = validationExecutor;
    }

    /**
     * 스토어 생성
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = {Exception.class},
        timeout = 30
    )
    public StoreCreatedDto createStore(Long ownerId, CreateStoreRequest request) {
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();
        
        log.info("[STORE_CREATE_START] correlationId={}, ownerId={}, storeName={}", 
                 correlationId, ownerId, request.getName());
        
        try {
            // 1단계: 기본 검증
            validateStoreCreation(ownerId, request.getName());
            
            // 2단계: 비동기 복합 검증
            ValidationResult validationResult = validateStoreCreationAsync(request, ownerId)
                .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
            if (!validationResult.isSuccess()) {
                throw validationResult.getException();
            }
            
            // 3단계: 스토어 생성 및 저장
            StoreCreatedDto result = performStoreCreation(ownerId, request, correlationId);
            
            // 4단계: 성공 로깅
            long duration = System.currentTimeMillis() - startTime;
            log.info("[STORE_CREATE_SUCCESS] correlationId={}, storeId={}, duration={}ms", 
                     correlationId, result.getId(), duration);
            
            return result;
            
        } catch (StoreException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_BUSINESS_ERROR] correlationId={}, ownerId={}, error={}, duration={}ms", 
                      correlationId, ownerId, e.getMessage(), duration, e);
            throw e;
            
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_TIMEOUT] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration);
            throw StoreException.validationTimeout();
            
        } catch (ExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            Throwable cause = e.getCause();
            if (cause instanceof StoreException) {
                log.error("[STORE_CREATE_ASYNC_ERROR] correlationId={}, ownerId={}, error={}, duration={}ms", 
                          correlationId, ownerId, cause.getMessage(), duration, cause);
                throw (StoreException) cause;
            }
            log.error("[STORE_CREATE_EXECUTION_ERROR] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration, e);
            throw StoreException.validationFailed("Validation failed", cause);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_INTERRUPTED] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration);
            throw StoreException.validationInterrupted();
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[STORE_CREATE_SYSTEM_ERROR] correlationId={}, ownerId={}, duration={}ms", 
                      correlationId, ownerId, duration, e);
            throw StoreException.validationFailed("스토어 생성 중 시스템 오류가 발생했습니다.", e);
        }
    }

    // ========================= 검증 로직 =========================

    /**
     * 기본 입력값 검증
     */
    private void validateStoreCreation(Long ownerId, String name) {
        validateStoreName(name);
        validateOwnerId(ownerId);
    }

    /**
     * 스토어 이름 검증
     */
    private void validateStoreName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw StoreException.emptyName();
        }
        
        String trimmedName = name.trim();
        if (trimmedName.length() < MIN_STORE_NAME_LENGTH || trimmedName.length() > MAX_STORE_NAME_LENGTH) {
            throw StoreException.invalidNameLength(trimmedName.length(), MIN_STORE_NAME_LENGTH, MAX_STORE_NAME_LENGTH);
        }
        
        if (containsInvalidCharacters(trimmedName)) {
            throw StoreException.invalidNameFormat(trimmedName);
        }
    }

    /**
     * 오너 ID 검증
     */
    private void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw StoreException.ownerNotFound();
        }
    }

    /**
     * 유효하지 않은 문자 포함 여부 확인
     */
    private boolean containsInvalidCharacters(String name) {
        String invalidChars = "<>\"'&;";
        return name.chars().anyMatch(c -> invalidChars.indexOf(c) >= 0);
    }

    /**
     * 중복 스토어 판단
     */
    private boolean isDuplicateStore(Optional<Store> existingStore) {
        return existingStore.isPresent() && !existingStore.get().isDeleted();
    }

    /**
     * 비동기 검증 로직
     */
    private CompletableFuture<ValidationResult> validateStoreCreationAsync(CreateStoreRequest request, Long ownerId) {
        CompletableFuture<Boolean> duplicateCheck = CompletableFuture.supplyAsync(() -> {
            Optional<Store> existingStore = storeRepository.findByName(request.getName());
            return isDuplicateStore(existingStore);
        }, validationExecutor);
        
        CompletableFuture<Boolean> permissionCheck = CompletableFuture.supplyAsync(() -> {
            return validateOwnerPermissionExternal(ownerId);
        }, validationExecutor);
        
        CompletableFuture<Integer> storeCountCheck = CompletableFuture.supplyAsync(() -> {
            return (int) storeRepository.countByOwnerId(ownerId);
        }, validationExecutor);
        
        return CompletableFuture.allOf(duplicateCheck, permissionCheck, storeCountCheck)
            .thenApply(v -> {
                try {
                    boolean isDuplicate = duplicateCheck.get();
                    boolean hasPermission = permissionCheck.get();
                    int storeCount = storeCountCheck.get();
                    
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
     * 비즈니스 규칙 검증
     */
    private void validateBusinessRules(String name, Long ownerId, boolean isDuplicate, boolean hasPermission, int storeCount) {
        if (isDuplicate) {
            throw StoreException.duplicateStoreName(name);
        }
        
        if (!hasPermission) {
            throw StoreException.ownerNotAuthorized(ownerId);
        }
        
        if (storeCount >= MAX_STORES_PER_OWNER) {
            throw StoreException.storeCreationLimitExceeded(ownerId, MAX_STORES_PER_OWNER);
        }
    }

    /**
     * 외부 권한 검증
     */
    private boolean validateOwnerPermissionExternal(Long ownerId) {
        return ownerId != null && ownerId > 0;
    }

    // ========================= 스토어 생성 로직 =========================

    /**
     * 스토어 생성 및 저장
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

    // ========================= 내부 클래스 =========================

    /**
     * 검증 결과
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