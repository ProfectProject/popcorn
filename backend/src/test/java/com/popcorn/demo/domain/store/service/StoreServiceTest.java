package com.popcorn.demo.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.*;

import com.popcorn.demo.domain.store.dto.StoreDetailDto;
import com.popcorn.demo.domain.store.dto.StoreListDto;
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

    private Long userId1;
    private Long userId2;
    private UUID storeId1;
    private UUID storeId2;
    private Store activeStore;
    private Store draftStore;


    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository);

        userId1 = 123L;
        userId2 = 456L;
        storeId1 = UUID.randomUUID();
        storeId2 = UUID.randomUUID();

        activeStore = Store.builder()
                .id(storeId1)
                .name("활성 팝콘 스토어")
                .ownerId(userId1)
                .publishStatus(StorePublishStatus.ACTIVE)
                .createdBy(userId1)
                .updatedBy(userId1)
                .build();

        draftStore = Store.builder()
                .id(storeId2)
                .name("임시 팝콘 스토어")
                .ownerId(userId1)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(userId2)
                .updatedBy(userId2)
                .build();
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

    @Test
    @DisplayName("내 스토어 목록 조회 성공")
    void 내_스토어_목록_조회_성공() {

        List<Store> storeList = Arrays.asList(activeStore, draftStore);

        when(storeRepository.findAllByOwnerId(userId1)).thenReturn(storeList);

        List<StoreListDto> result = storeService.getStoresByOwnerId(userId1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(storeId1);
        assertThat(result.get(0).getName()).isEqualTo("활성 팝콘 스토어");
        assertThat(result.get(0).getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
        assertThat(result.get(1).getId()).isEqualTo(storeId2);
        assertThat(result.get(1).getName()).isEqualTo("임시 팝콘 스토어");
        assertThat(result.get(1).getPublishStatus()).isEqualTo(StorePublishStatus.DRAFT);

    }

    @Test
    @DisplayName("내 스토어 목록 조회 성공 - 빈 목록")
    void 내_스토어_목록_조회_성공_빈목록() {

        when(storeRepository.findAllByOwnerIdAndDeletedAtIsNull(userId1)).thenReturn(Collections.emptyList());

        List<StoreListDto> result = storeService.getStoresByOwnerId(userId1);

        assertThat(result).isEmpty();

    }

    @Test
    @DisplayName("내 스토어 목록 조회 성공 - 삭제된 스토어 제외 ")
    void 내_스토어_목록_조회_성공_삭제된스토어제외() {

        Store deletedStore = Store.builder()
                .id(UUID.randomUUID())
                .name("삭제된 팝콘 스토어")
                .ownerId(userId1)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(userId1)
                .updatedBy(userId1)
                .deletedAt(LocalDateTime.now())
                .deletedBy(userId1)
                .build();

        List<Store> storeList = Arrays.asList(activeStore, deletedStore);

        when(storeRepository.findAllByOwnerIdAndDeletedAtIsNull(userId1)).thenReturn(storeList);

        List<StoreListDto> result = storeService.getStoresByOwnerId(userId1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(storeId1);
        assertThat(result.get(0).getName()).isEqualTo("활성 팝콘 스토어");
        assertThat(result.get(0).getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);

    }

    @Test
    @DisplayName("내 스토어 목록 조회 실패 - null 오너 ID")
    void 내_스토어_목록_조회_실패_null_오너_ID() {
        // Given
        Long invalidOwnerId = null;

        // When & Then
        assertThatThrownBy(() -> storeService.getStoresByOwnerId(invalidOwnerId))
                .isInstanceOf(StoreException.class);
    }

    @Test
    @DisplayName("스토어 상세 조회 성공")
    void 스토어_상세_조회_성공() {

        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        StoreDetailDto result = storeService.getStoreDetail(userId1, storeId1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(storeId1);
        assertThat(result.getName()).isEqualTo("활성 팝콘 스토어");
        assertThat(result.getOwnerId()).isEqualTo(userId1);
        assertThat(result.getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
        assertThat(result.getCreatedAt()).isNotNull();

    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - 존재하지 않는 스토어")
    void 스토어_상세_조회_실패_존재하지_않는_스토어() {

        UUID storeId = UUID.randomUUID();

        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.getStoreDetail(userId1, storeId))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("스토어를 찾을 수 없습니다.");

    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - 권한 없음")
    void 스토어_상세_조회_실패_권한_없음() {

        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.getStoreDetail(userId2, storeId1))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("권한이 없습니다.");

    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - null 파라미터")
    void 스토어_상세_조회_실패_null_파라미터() {

        assertThatThrownBy(() -> storeService.getStoreDetail(userId1, null))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("스토어 ID는 필수입니다.");

        assertThatThrownBy(() -> storeService.getStoreDetail(null, storeId1))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("사용자 ID는 필수입니다.");

    }



    }