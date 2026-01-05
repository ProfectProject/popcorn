package com.popcorn.demo.domain.store.service;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private static final int MAX_STORES_PER_OWNER = 10;
    private static final int MAX_STORE_NAME_LENGTH = 100;
    private static final int MIN_STORE_NAME_LENGTH = 1;

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreCreatedDto createStore(Long ownerId, CreateStoreRequest request) {
        log.info("[STORE_CREATE_START] ownerId={}, storeName={}", ownerId, request.getName());
        
        validateStoreCreation(ownerId, request.getName());
        
        Optional<Store> existingStore = storeRepository.findByName(request.getName().trim());
        if (existingStore.isPresent() && !existingStore.get().isDeleted()) {
            log.warn("[STORE_CREATE_DUPLICATE] storeName={}", request.getName());
            throw StoreException.duplicateStoreName(request.getName());
        }
        
        long storeCount = storeRepository.countByOwnerId(ownerId);
        if (storeCount >= MAX_STORES_PER_OWNER) {
            log.warn("[STORE_CREATE_LIMIT_EXCEEDED] ownerId={}, currentCount={}, maxAllowed={}", 
                     ownerId, storeCount, MAX_STORES_PER_OWNER);
            throw StoreException.storeCreationLimitExceeded(ownerId, MAX_STORES_PER_OWNER);
        }
        
        Store store = Store.builder()
                .name(request.getName().trim())
                .ownerId(ownerId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(ownerId)
                .updatedBy(ownerId)
                .build();

        Store savedStore = storeRepository.save(store);
        
        log.info("[STORE_CREATE_SUCCESS] storeId={}, ownerId={}", savedStore.getId(), ownerId);

        return StoreCreatedDto.builder()
                .id(savedStore.getId())
                .name(savedStore.getName())
                .ownerId(savedStore.getOwnerId())
                .publishStatus(savedStore.getPublishStatus())
                .createdAt(savedStore.getCreatedAt())
                .createdBy(savedStore.getCreatedBy())
                .build();
    }

    private void validateStoreCreation(Long ownerId, String name) {
        validateStoreName(name);
        validateOwnerId(ownerId);
    }

    private void validateStoreName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("[STORE_VALIDATION_FAILED] Empty store name");
            throw StoreException.emptyName();
        }
        
        String trimmedName = name.trim();
        if (trimmedName.length() < MIN_STORE_NAME_LENGTH || trimmedName.length() > MAX_STORE_NAME_LENGTH) {
            log.warn("[STORE_VALIDATION_FAILED] Invalid name length: actual={}, min={}, max={}", 
                     trimmedName.length(), MIN_STORE_NAME_LENGTH, MAX_STORE_NAME_LENGTH);
            throw StoreException.invalidNameLength(trimmedName.length(), MIN_STORE_NAME_LENGTH, MAX_STORE_NAME_LENGTH);
        }
        
        if (containsInvalidCharacters(trimmedName)) {
            log.warn("[STORE_VALIDATION_FAILED] Invalid characters in name: {}", trimmedName);
            throw StoreException.invalidNameFormat(trimmedName);
        }
    }

    private void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            log.warn("[STORE_VALIDATION_FAILED] Invalid ownerId: {}", ownerId);
            throw StoreException.ownerNotFound();
        }
    }

    private boolean containsInvalidCharacters(String name) {
        String invalidChars = "<>\"'&;";
        return name.chars().anyMatch(c -> invalidChars.indexOf(c) >= 0);
    }
}