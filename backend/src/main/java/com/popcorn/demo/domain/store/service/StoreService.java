package com.popcorn.demo.domain.store.service;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.dto.StoreDetailDto;
import com.popcorn.demo.domain.store.dto.StoreListDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private static final int MAX_STORES_PER_OWNER = 10;
    private static final int MAX_STORE_NAME_LENGTH = 100;
    private static final int MIN_STORE_NAME_LENGTH = 1;
    private static final String INVALID_CHARS = "<>\"'&;";

    private final StoreRepository storeRepository;

    @Transactional
    public StoreCreatedDto createStore(Long ownerId, CreateStoreRequest request) {
        log.info("[STORE_CREATE] ownerId={}, name={}", ownerId, request.getName());
        
        String trimmedName = validateAndTrimName(request.getName());
        validateOwnerId(ownerId);
        checkDuplicateName(trimmedName);
        checkStoreLimit(ownerId);
        
        Store savedStore = storeRepository.save(createStoreEntity(ownerId, trimmedName));
        
        log.info("[STORE_CREATED] storeId={}", savedStore.getId());
        return mapToDto(savedStore);
    }

    @Transactional(readOnly = true)
    public StoreListDto getStore(Long ownerId) {
        log.info("[STORE_GET] userID={}", ownerId);


    }

    @Transactional(readOnly = true)
    public StoreDetailDto getStoreDetail(Long userId, UUID storeId) {
        log.info("[STORE_DETAIL_GET] userId={}, storeId={}", userId, storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> StoreException.storeNotFound(storeId));

        if (!store.isOwner(userId)) {
            throw StoreException.accessDenied(userId, storeId);
        }

        return mapToDetailDto(store);
    }

    private String validateAndTrimName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw StoreException.emptyName();
        }
        
        String trimmed = name.trim();
        if (trimmed.length() < MIN_STORE_NAME_LENGTH || trimmed.length() > MAX_STORE_NAME_LENGTH) {
            throw StoreException.invalidNameLength(trimmed.length(), MIN_STORE_NAME_LENGTH, MAX_STORE_NAME_LENGTH);
        }
        
        if (trimmed.chars().anyMatch(c -> INVALID_CHARS.indexOf(c) >= 0)) {
            throw StoreException.invalidNameFormat(trimmed);
        }
        
        return trimmed;
    }

    private void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw StoreException.ownerNotFound();
        }
    }

    private void checkDuplicateName(String name) {
        storeRepository.findByName(name)
                .filter(store -> !store.isDeleted())
                .ifPresent(store -> {
                    throw StoreException.duplicateStoreName(name);
                });
    }

    private void checkStoreLimit(Long ownerId) {
        long storeCount = storeRepository.countByOwnerId(ownerId);
        if (storeCount >= MAX_STORES_PER_OWNER) {
            throw StoreException.storeCreationLimitExceeded(ownerId, MAX_STORES_PER_OWNER);
        }
    }

    private Store createStoreEntity(Long ownerId, String name) {
        return Store.builder()
                .name(name)
                .ownerId(ownerId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(ownerId)
                .updatedBy(ownerId)
                .build();
    }

    private StoreCreatedDto mapToDto(Store store) {
        return StoreCreatedDto.builder()
                .id(store.getId())
                .name(store.getName())
                .ownerId(store.getOwnerId())
                .publishStatus(store.getPublishStatus())
                .createdAt(store.getCreatedAt())
                .createdBy(store.getCreatedBy())
                .build();
    }

    private StoreListDto mapTODto(Store store) {
        return StoreListDto.builder()
                .id(store.getId())
                .name(store.getName())
                .publishStatus(store.getPublishStatus())
                .createdAt(store.getCreatedAt())
                .build();
    }

    private StoreDetailDto mapToDetailDto(Store store) {
        return StoreDetailDto.builder()
                .id(store.getId())
                .name(store.getName())
                .publishStatus(store.getPublishStatus())
                .createdAt(store.getCreatedAt())
                .build();
    }
}