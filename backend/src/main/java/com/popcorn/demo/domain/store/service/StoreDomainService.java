package com.popcorn.demo.domain.store.service;

import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.exception.StoreException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StoreDomainService {

    public boolean isDuplicateStore(Optional<Store> existingStore, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }

        return existingStore.isPresent();

    }

    public void validateStoreCreation(Long ownerId, String name) {

        if (name == null || name.trim().isEmpty()) {
            throw StoreException.emptyName();
        }

        if (ownerId == null || ownerId <= 0) {
            throw new IllegalArgumentException("유효한 오너 ID를 입력해주세요.");
        }
    }

}
