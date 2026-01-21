package com.popcorn.store.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.store.domain.store.dto.CreateStoreRequest;
import com.popcorn.store.domain.store.dto.StoreCreatedDto;
import com.popcorn.store.domain.store.dto.StoreDeletedDto;
import com.popcorn.store.domain.store.dto.StoreDetailDto;
import com.popcorn.store.domain.store.dto.StoreListDto;
import com.popcorn.store.domain.store.dto.StoreResponseCode;
import com.popcorn.store.domain.store.dto.StoreStatusUpdatedDto;
import com.popcorn.store.domain.store.dto.StoreUpdatedDto;
import com.popcorn.store.domain.store.dto.UpdateStoreRequest;
import com.popcorn.store.domain.store.dto.UpdateStoreStatusRequest;
import com.popcorn.store.domain.store.entity.Store;
import com.popcorn.store.domain.store.entity.StorePublishStatus;
import com.popcorn.store.domain.store.exception.StoreException;
import com.popcorn.store.domain.store.repository.StoreRepository;

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
    void createsStore() {
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("테스트 스토어")
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

        StoreCreatedDto result = storeService.createStore(ownerId, request);

        assertThat(result.getId()).isEqualTo(savedStore.getId());
        assertThat(result.getName()).isEqualTo("테스트 스토어");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getPublishStatus()).isEqualTo(StorePublishStatus.DRAFT);
    }

    @Test
    @DisplayName("스토어 생성 실패 - 빈 이름")
    void createStoreFailsWhenNameEmpty() {
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("")
                .build();

        assertThatThrownBy(() -> storeService.createStore(ownerId, request))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.EMPTY_NAME);
    }

    @Test
    @DisplayName("스토어 생성 실패 - null 오너 ID")
    void createStoreFailsWhenOwnerMissing() {
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("테스트 스토어")
                .build();

        assertThatThrownBy(() -> storeService.createStore(null, request))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.OWNER_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 생성 실패 - 중복 이름")
    void createStoreFailsWhenDuplicateName() {
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("중복 스토어")
                .build();

        when(storeRepository.findByName("중복 스토어")).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.createStore(ownerId, request))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.DUPLICATE_STORE_NAME);
    }

    @Test
    @DisplayName("스토어 생성 실패 - 생성 한도 초과")
    void createStoreFailsWhenLimitExceeded() {
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("한도 초과 스토어")
                .build();

        when(storeRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(storeRepository.countByOwnerId(ownerId)).thenReturn(10L);

        assertThatThrownBy(() -> storeService.createStore(ownerId, request))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_CREATION_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("스토어 생성 실패 - 이름 형식 오류")
    void createStoreFailsWhenNameInvalid() {
        Long ownerId = 123L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("잘못된&스토어")
                .build();

        assertThatThrownBy(() -> storeService.createStore(ownerId, request))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.INVALID_NAME_FORMAT);
    }

    @Test
    @DisplayName("내 스토어 목록 조회 성공")
    void getStoresByOwnerIdReturnsStores() {
        List<Store> storeList = Arrays.asList(activeStore, draftStore);

        when(storeRepository.findAllByOwnerIdAndDeletedAtIsNull(userId1)).thenReturn(storeList);

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
    void getStoresByOwnerIdReturnsEmpty() {
        when(storeRepository.findAllByOwnerIdAndDeletedAtIsNull(userId1)).thenReturn(Collections.emptyList());

        List<StoreListDto> result = storeService.getStoresByOwnerId(userId1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("내 스토어 목록 조회 성공 - 삭제된 스토어 제외")
    void getStoresByOwnerIdSkipsDeletedStore() {
        Store deletedStore = Store.builder()
                .id(UUID.randomUUID())
                .name("삭제된 팝콘 스토어")
                .ownerId(userId1)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdBy(userId1)
                .updatedBy(userId1)
                .build();
        deletedStore.delete(userId1);

        when(storeRepository.findAllByOwnerIdAndDeletedAtIsNull(userId1))
                .thenReturn(Arrays.asList(activeStore, deletedStore));

        List<StoreListDto> result = storeService.getStoresByOwnerId(userId1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(storeId1);
        assertThat(result.get(0).getName()).isEqualTo("활성 팝콘 스토어");
        assertThat(result.get(0).getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
    }

    @Test
    @DisplayName("내 스토어 목록 조회 실패 - null 오너 ID")
    void getStoresByOwnerIdFailsWhenOwnerMissing() {
        assertThatThrownBy(() -> storeService.getStoresByOwnerId(null))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.OWNER_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 상세 조회 성공")
    void getStoreDetailReturnsDetail() {
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
    void getStoreDetailFailsWhenStoreMissing() {
        when(storeRepository.findById(storeId1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.getStoreDetail(userId1, storeId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - 권한 없음")
    void getStoreDetailFailsWhenNotOwner() {
        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.getStoreDetail(userId2, storeId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - null 파라미터")
    void getStoreDetailFailsWhenParamsMissing() {
        assertThatThrownBy(() -> storeService.getStoreDetail(userId1, null))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_ID_REQUIRED);

        assertThatThrownBy(() -> storeService.getStoreDetail(null, storeId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.USER_ID_REQUIRED);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 성공")
    void updateStoreUpdatesData() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("수정된 스토어")
                .build();

        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));
        when(storeRepository.findByName("수정된 스토어")).thenReturn(Optional.empty());
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreUpdatedDto result = storeService.updateStore(storeId1, request, userId1);

        assertThat(result.getId()).isEqualTo(storeId1);
        assertThat(result.getName()).isEqualTo("수정된 스토어");
        assertThat(result.getUpdatedBy()).isEqualTo(userId1);
        assertThat(activeStore.getName()).isEqualTo("수정된 스토어");
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 실패 - 빈 이름")
    void updateStoreFailsWhenNameEmpty() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("")
                .build();

        assertThatThrownBy(() -> storeService.updateStore(storeId1, request, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.EMPTY_NAME);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 실패 - null 사용자 ID")
    void updateStoreFailsWhenOwnerMissing() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("수정된 스토어")
                .build();

        assertThatThrownBy(() -> storeService.updateStore(storeId1, request, null))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.OWNER_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 실패 - 존재하지 않는 스토어")
    void updateStoreFailsWhenStoreMissing() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("수정된 스토어")
                .build();

        when(storeRepository.findById(storeId1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.updateStore(storeId1, request, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 실패 - 권한 없음")
    void updateStoreFailsWhenNotOwner() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("수정된 스토어")
                .build();

        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.updateStore(storeId1, request, userId2))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 실패 - 삭제된 스토어")
    void updateStoreFailsWhenDeletedStore() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("수정된 스토어")
                .build();

        activeStore.delete(userId1);
        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.updateStore(storeId1, request, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_ALREADY_DELETED);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 실패 - 중복 이름")
    void updateStoreFailsWhenDuplicateName() {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("중복 스토어")
                .build();

        Store otherStore = Store.builder()
                .id(UUID.randomUUID())
                .name("중복 스토어")
                .ownerId(userId2)
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));
        when(storeRepository.findByName("중복 스토어")).thenReturn(Optional.of(otherStore));

        assertThatThrownBy(() -> storeService.updateStore(storeId1, request, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.DUPLICATE_STORE_NAME);
    }

    @Test
    @DisplayName("스토어 상태 수정 성공")
    void updateStoreStatusUpdatesStatus() {
        UpdateStoreStatusRequest request = UpdateStoreStatusRequest.builder()
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        when(storeRepository.findById(storeId2)).thenReturn(Optional.of(draftStore));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreStatusUpdatedDto result = storeService.updateStoreStatus(storeId2, request, userId1);

        assertThat(result.getId()).isEqualTo(storeId2);
        assertThat(result.getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
        assertThat(result.getUpdatedBy()).isEqualTo(userId1);
        assertThat(draftStore.getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
    }

    @Test
    @DisplayName("스토어 상태 수정 실패 - null 사용자 ID")
    void updateStoreStatusFailsWhenOwnerMissing() {
        UpdateStoreStatusRequest request = UpdateStoreStatusRequest.builder()
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> storeService.updateStoreStatus(storeId1, request, null))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.OWNER_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 상태 수정 실패 - 존재하지 않는 스토어")
    void updateStoreStatusFailsWhenStoreMissing() {
        UpdateStoreStatusRequest request = UpdateStoreStatusRequest.builder()
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        when(storeRepository.findById(storeId1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.updateStoreStatus(storeId1, request, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 상태 수정 실패 - 권한 없음")
    void updateStoreStatusFailsWhenNotOwner() {
        UpdateStoreStatusRequest request = UpdateStoreStatusRequest.builder()
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.updateStoreStatus(storeId1, request, userId2))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("스토어 상태 수정 실패 - 삭제된 스토어")
    void updateStoreStatusFailsWhenDeletedStore() {
        UpdateStoreStatusRequest request = UpdateStoreStatusRequest.builder()
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        activeStore.delete(userId1);
        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.updateStoreStatus(storeId1, request, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_ALREADY_DELETED);
    }

    @Test
    @DisplayName("스토어 삭제 성공")
    void deleteStoreMarksDeleted() {
        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreDeletedDto result = storeService.deleteStore(storeId1, userId1);

        assertThat(result.getId()).isEqualTo(storeId1);
        assertThat(result.getDeletedBy()).isEqualTo(userId1);
        assertThat(result.getDeletedAt()).isNotNull();
        assertThat(activeStore.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("스토어 삭제 실패 - null 사용자 ID")
    void deleteStoreFailsWhenOwnerMissing() {
        assertThatThrownBy(() -> storeService.deleteStore(storeId1, null))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.OWNER_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 삭제 실패 - 존재하지 않는 스토어")
    void deleteStoreFailsWhenStoreMissing() {
        when(storeRepository.findById(storeId1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.deleteStore(storeId1, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("스토어 삭제 실패 - 권한 없음")
    void deleteStoreFailsWhenNotOwner() {
        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.deleteStore(storeId1, userId2))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("스토어 삭제 실패 - 이미 삭제됨")
    void deleteStoreFailsWhenAlreadyDeleted() {
        activeStore.delete(userId1);
        when(storeRepository.findById(storeId1)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> storeService.deleteStore(storeId1, userId1))
                .isInstanceOf(StoreException.class)
                .extracting("responseCode")
                .isEqualTo(StoreResponseCode.STORE_ALREADY_DELETED);
    }
}
