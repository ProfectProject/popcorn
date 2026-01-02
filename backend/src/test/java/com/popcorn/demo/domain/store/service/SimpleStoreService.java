package com.popcorn.demo.domain.store.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;

@Service("simpleStoreService")
public class SimpleStoreService {

    private final StoreRepository storeRepository;

    public SimpleStoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreCreatedDto createStore(Long ownerId, CreateStoreRequest request) {
        // 기본 검증
        validateStoreCreation(ownerId, request.getName());
        
        // 중복 검사
        Optional<Store> existingStore = storeRepository.findByName(request.getName());
        if (existingStore.isPresent() && !existingStore.get().isDeleted()) {
            throw StoreException.duplicateStoreName(request.getName());
        }
        
        // 한도 검사
        long storeCount = storeRepository.countByOwnerId(ownerId);
        if (storeCount >= 10) {
            throw StoreException.storeCreationLimitExceeded(ownerId, 10);
        }
        
        // 스토어 생성
        Store store = Store.builder()
                .name(request.getName().trim())
                .ownerId(ownerId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(ownerId)
                .updatedBy(ownerId)
                .build();

        Store savedStore = storeRepository.save(store);

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
        if (name == null || name.trim().isEmpty()) {
            throw StoreException.emptyName();
        }
        
        if (ownerId == null || ownerId <= 0) {
            throw StoreException.ownerNotFound();
        }
        
        String trimmedName = name.trim();
        if (trimmedName.length() > 100) {
            throw StoreException.invalidNameLength(trimmedName.length(), 1, 100);
        }
        
        if (containsInvalidCharacters(trimmedName)) {
            throw StoreException.invalidNameFormat(trimmedName);
        }
    }

    private boolean containsInvalidCharacters(String name) {
        String invalidChars = "<>\"'&;";
        return name.chars().anyMatch(c -> invalidChars.indexOf(c) >= 0);
    }
}