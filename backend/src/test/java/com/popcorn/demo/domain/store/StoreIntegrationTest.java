package com.popcorn.demo.domain.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;
import com.popcorn.demo.domain.store.service.StoreService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoreIntegrationTest {

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    @DisplayName("스토어 생성 전체 플로우 성공")
    void 스토어_생성_전체_플로우_성공() {
        // Given
        Long ownerId = 1001L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("통합테스트 팝콘 스토어")
                .ownerId(ownerId)
                .build();

        // When
        StoreCreatedDto result = storeService.createStore(ownerId, request);

        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("통합테스트 팝콘 스토어");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getPublishStatus()).isEqualTo(StorePublishStatus.DRAFT);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getCreatedBy()).isEqualTo(ownerId);

        // 데이터베이스에 실제 저장 확인
        Store savedStore = storeRepository.findById(result.getId()).orElse(null);
        assertThat(savedStore).isNotNull();
        assertThat(savedStore.getName()).isEqualTo("통합테스트 팝콘 스토어");
        assertThat(savedStore.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("중복된 스토어 이름으로 생성 실패")
    void 중복된_스토어_이름으로_생성_실패() {
        // Given
        Long firstOwnerId = 1001L;
        Long secondOwnerId = 1002L;
        String storeName = "중복테스트 스토어";

        // 첫 번째 스토어 생성
        CreateStoreRequest firstRequest = CreateStoreRequest.builder()
                .name(storeName)
                .ownerId(firstOwnerId)
                .build();
        storeService.createStore(firstOwnerId, firstRequest);

        // 같은 이름으로 두 번째 스토어 생성 시도
        CreateStoreRequest secondRequest = CreateStoreRequest.builder()
                .name(storeName)
                .ownerId(secondOwnerId)
                .build();

        // When & Then
        assertThatThrownBy(() -> storeService.createStore(secondOwnerId, secondRequest))
                .isInstanceOf(StoreException.class);
    }
}