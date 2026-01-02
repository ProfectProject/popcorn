package com.popcorn.demo.domain.store.service;

import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.exception.StoreException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 스토어 도메인 서비스
 * - 순수 비즈니스 로직만 포함
 * - 인프라스트럭처에 의존하지 않는 도메인 규칙 검증
 * - 스토어 생성/수정/삭제 관련 비즈니스 불변식 관리
 */
@Service
public class StoreDomainService {

    private static final int MAX_STORE_NAME_LENGTH = 100;
    private static final int MIN_STORE_NAME_LENGTH = 1;

    /**
     * 멱등성 키를 기반으로 중복 스토어 여부를 판단합니다.
     *
     * @param existingStore 기존 스토어 (Optional)
     * @param idempotencyKey 멱등성 키
     * @return 중복 스토어 여부
     */
    public boolean isDuplicateStore(Optional<Store> existingStore, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return existingStore.isPresent() && !existingStore.get().isDeleted();
        }
        return existingStore.isPresent();
    }

    /**
     * 스토어 생성 전 기본 입력값 검증을 수행합니다.
     *
     * @param ownerId 오너 ID
     * @param name 스토어 이름
     * @throws StoreException 검증 실패 시
     */
    public void validateStoreCreation(Long ownerId, String name) {
        validateStoreName(name);
        validateOwnerId(ownerId);
    }

    /**
     * 스토어 이름 유효성 검증
     *
     * @param name 스토어 이름
     * @throws StoreException 검증 실패 시
     */
    public void validateStoreName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw StoreException.emptyName();
        }

        String trimmedName = name.trim();

        // 특수문자 검증
        if (containsInvalidCharacters(trimmedName)) {
            throw StoreException.invalidNameFormat(trimmedName);
        }
    }

    /**
     * 오너 ID 유효성 검증
     *
     * @param ownerId 오너 ID
     * @throws StoreException 검증 실패 시
     */
    public void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw StoreException.ownerNotFound();
        }
    }

    /**
     * 스토어 이름에 유효하지 않은 문자가 포함되어 있는지 확인
     *
     * @param name 스토어 이름
     * @return 유효하지 않은 문자 포함 여부
     */
    private boolean containsInvalidCharacters(String name) {
        // 기본적인 특수문자 제한 (필요에 따라 조정)
        String invalidChars = "<>\"'&;";
        return name.chars().anyMatch(c -> invalidChars.indexOf(c) >= 0);
    }

    /**
     * 스토어 삭제 가능 여부 검증
     *
     * @param store 스토어 엔티티
     * @param requesterId 삭제 요청자 ID
     * @return 삭제 가능 여부
     */
    public boolean canDeleteStore(Store store, Long requesterId) {
        if (store == null || store.isDeleted()) {
            return false;
        }
        
        // 오너만 삭제 가능
        return store.isOwner(requesterId);
    }

    /**
     * 스토어 수정 가능 여부 검증
     *
     * @param store 스토어 엔티티
     * @param requesterId 수정 요청자 ID
     * @return 수정 가능 여부
     */
    public boolean canUpdateStore(Store store, Long requesterId) {
        if (store == null || store.isDeleted()) {
            return false;
        }
        
        // 오너만 수정 가능
        return store.isOwner(requesterId);
    }

    /**
     * 스토어 발행 상태 변경 가능 여부 검증
     *
     * @param store 스토어 엔티티
     * @param requesterId 요청자 ID
     * @return 상태 변경 가능 여부
     */
    public boolean canChangePublishStatus(Store store, Long requesterId) {
        return canUpdateStore(store, requesterId);
    }

}
