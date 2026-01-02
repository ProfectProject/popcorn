package com.popcorn.demo.domain.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.entity.Store;

/**
 * 스토어 레포지토리 인터페이스
 * - 클린 아키텍처: 도메인 계층에서 인터페이스 정의
 * - 인프라스트럭처 계층에서 구현체 제공
 * - 도메인 엔티티 Store를 다루는 데이터 액세스 추상화
 */
public interface StoreRepository {

    // ========================= 기본 CRUD 메서드 =========================

    /**
     * 스토어 저장 (생성/수정)
     * @param store 저장할 스토어 엔티티
     * @return 저장된 스토어 엔티티 (ID 할당됨)
     */
    Store save(Store store);

    /**
     * 스토어 ID로 조회
     * @param id 스토어 ID (UUID)
     * @return 스토어 엔티티 (Optional)
     */
    Optional<Store> findById(UUID id);

    /**
     * 스토어 삭제
     * @param id 삭제할 스토어 ID
     */
    void deleteById(UUID id);

    /**
     * 스토어 존재 여부 확인
     * @param id 스토어 ID
     * @return 존재하면 true
     */
    boolean existsById(UUID id);

    // ========================= 비즈니스 조회 메서드 =========================

    /**
     * 오너의 모든 스토어 조회
     * @param ownerId 오너 ID
     * @return 오너의 스토어 목록
     */
    List<Store> findAllByOwnerId(Long ownerId);

    /**
     * 오너의 삭제되지 않은 스토어 조회
     * @param ownerId 오너 ID
     * @return 삭제되지 않은 스토어 목록
     */
    List<Store> findAllByOwnerIdAndDeletedAtIsNull(Long ownerId);

    /**
     * 특정 상태의 오너 스토어 조회
     * @param status 발행 상태
     * @param ownerId 오너 ID
     * @return 해당 상태의 스토어 목록
     */
    List<Store> findByPublishStatusAndOwnerId(StorePublishStatus status, Long ownerId);

    /**
     * 스토어 이름으로 조회
     * @param name 스토어 이름
     * @return 스토어 엔티티 (Optional)
     */
    Optional<Store> findByName(String name);

    // ========================= 통계 및 집계 메서드 =========================

    /**
     * 오너의 총 스토어 수 조회
     * @param ownerId 오너 ID
     * @return 총 스토어 수
     */
    long countByOwnerId(Long ownerId);

    /**
     * 특정 상태의 스토어 수 조회
     * @param status 발행 상태
     * @return 해당 상태의 스토어 수
     */
    long countByPublishStatus(StorePublishStatus status);

}