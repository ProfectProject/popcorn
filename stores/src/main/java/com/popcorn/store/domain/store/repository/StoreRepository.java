package com.popcorn.store.domain.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.popcorn.store.domain.store.entity.Store;
import com.popcorn.store.domain.store.entity.StorePublishStatus;

public interface StoreRepository {

    Store save(Store store);
    Optional<Store> findById(UUID id);
    void deleteById(UUID id);
    boolean existsById(UUID id);

    List<Store> findAllByOwnerId(Long ownerId);
    List<Store> findAllByOwnerIdAndDeletedAtIsNull(Long ownerId);
    List<Store> findByPublishStatusAndOwnerId(StorePublishStatus status, Long ownerId);
    Optional<Store> findByName(String name);

    long countByOwnerId(Long ownerId);
    long countByPublishStatus(StorePublishStatus status);
}