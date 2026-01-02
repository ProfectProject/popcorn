package com.popcorn.demo.domain.store.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;

/**
 * Store JPA Repository
 * - Spring Data JPA 기반 데이터 액세스
 */
public interface JpaStoreRepository extends JpaRepository<Store, UUID> {

    /**
     * 오너의 모든 스토어 조회
     */
    List<Store> findAllByOwnerId(Long ownerId);

    /**
     * 오너의 삭제되지 않은 스토어 조회
     */
    List<Store> findAllByOwnerIdAndDeletedAtIsNull(Long ownerId);

    /**
     * 특정 상태의 오너 스토어 조회
     */
    List<Store> findByPublishStatusAndOwnerId(StorePublishStatus status, Long ownerId);

    /**
     * 스토어 이름으로 조회
     */
    Optional<Store> findByName(String name);

    /**
     * 오너의 총 스토어 수 조회
     */
    long countByOwnerId(Long ownerId);

    /**
     * 특정 상태의 스토어 수 조회
     */
    long countByPublishStatus(StorePublishStatus status);
}