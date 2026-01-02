package com.popcorn.demo.domain.store.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

/**
 * StoreRepository JPA 구현체
 * - JpaStoreRepository를 사용하여 실제 데이터 액세스 구현
 */
@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepository {

    private final JpaStoreRepository jpaStoreRepository;

    @Override
    public Store save(Store store) {
        return jpaStoreRepository.save(store);
    }

    @Override
    public Optional<Store> findById(UUID id) {
        return jpaStoreRepository.findById(id);
    }

    @Override
    public void deleteById(UUID id) {
        jpaStoreRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaStoreRepository.existsById(id);
    }

    @Override
    public List<Store> findAllByOwnerId(Long ownerId) {
        return jpaStoreRepository.findAllByOwnerId(ownerId);
    }

    @Override
    public List<Store> findAllByOwnerIdAndDeletedAtIsNull(Long ownerId) {
        return jpaStoreRepository.findAllByOwnerIdAndDeletedAtIsNull(ownerId);
    }

    @Override
    public List<Store> findByPublishStatusAndOwnerId(StorePublishStatus status, Long ownerId) {
        return jpaStoreRepository.findByPublishStatusAndOwnerId(status, ownerId);
    }

    @Override
    public Optional<Store> findByName(String name) {
        return jpaStoreRepository.findByName(name);
    }

    @Override
    public long countByOwnerId(Long ownerId) {
        return jpaStoreRepository.countByOwnerId(ownerId);
    }

    @Override
    public long countByPublishStatus(StorePublishStatus status) {
        return jpaStoreRepository.countByPublishStatus(status);
    }
}