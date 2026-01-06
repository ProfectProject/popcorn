package com.popcorn.demo.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository);
    }

    @Test
    @DisplayName("스토어 생성 성공")
    void 스토어_생성_성공() {
        // Given
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("테스트 스토어")
                .ownerId(ownerId)
                .build();

        Store savedStore = Store.builder()
                .id(UUID.randomUUID())
                .name("테스트 스토어")
                .ownerId(ownerId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(ownerId)
                .updatedBy(ownerId)
                .build();

        when(storeRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(storeRepository.countByOwnerId(ownerId)).thenReturn(0L);
        when(storeRepository.save(any(Store.class))).thenReturn(savedStore);

        // When
        StoreCreatedDto result = storeService.createStore(ownerId, request);

        // Then
        assertThat(result.getId()).isEqualTo(savedStore.getId());
        assertThat(result.getName()).isEqualTo("테스트 스토어");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getPublishStatus()).isEqualTo(StorePublishStatus.DRAFT);
    }

    @Test
    @DisplayName("스토어 생성 실패 - 빈 이름")
    void 스토어_생성_실패_빈_이름() {
        // Given
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("")
                .ownerId(ownerId)
                .build();

        // When & Then
        assertThatThrownBy(() -> storeService.createStore(ownerId, request))
                .isInstanceOf(StoreException.class);
    }

    @Test
    @DisplayName("스토어 생성 실패 - null 오너 ID")
    void 스토어_생성_실패_null_오너_ID() {
        // Given
        Long invalidOwnerId = null;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("테스트 스토어")
                .ownerId(invalidOwnerId)
                .build();

        // When & Then
        assertThatThrownBy(() -> storeService.createStore(invalidOwnerId, request))
                .isInstanceOf(StoreException.class);
    }
}