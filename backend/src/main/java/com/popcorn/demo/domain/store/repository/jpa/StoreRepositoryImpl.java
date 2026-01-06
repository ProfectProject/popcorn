package com.popcorn.demo.domain.store.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.repository.StoreRepository;

@Repository
public class StoreRepositoryImpl implements StoreRepository {

    private final JpaStoreRepository jpaStoreRepository;

    public StoreRepositoryImpl(JpaStoreRepository jpaStoreRepository) {
        this.jpaStoreRepository = jpaStoreRepository;
    }

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
        return jpaStoreRepository.findAll().stream()
                .filter(store -> store.getOwnerId().equals(ownerId))
                .toList();
    }

    @Override
    public List<Store> findAllByOwnerIdAndDeletedAtIsNull(Long ownerId) {
        return jpaStoreRepository.findAll().stream()
                .filter(store -> store.getOwnerId().equals(ownerId) && !store.isDeleted())
                .toList();
    }

    @Override
    public List<Store> findByPublishStatusAndOwnerId(StorePublishStatus status, Long ownerId) {
        return jpaStoreRepository.findAll().stream()
                .filter(store -> store.getPublishStatus().equals(status) && store.getOwnerId().equals(ownerId))
                .toList();
    }

    @Override
    public Optional<Store> findByName(String name) {
        return jpaStoreRepository.findAll().stream()
                .filter(store -> store.getName().equals(name))
                .findFirst();
    }

    @Override
    public long countByOwnerId(Long ownerId) {
        return jpaStoreRepository.findAll().stream()
                .filter(store -> store.getOwnerId().equals(ownerId))
                .count();
    }

    @Override
    public long countByPublishStatus(StorePublishStatus status) {
        return jpaStoreRepository.findAll().stream()
                .filter(store -> store.getPublishStatus().equals(status))
                .count();
    }
}