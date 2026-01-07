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
import com.popcorn.demo.domain.store.dto.StoreDeletedDto;
import com.popcorn.demo.domain.store.dto.StoreDetailDto;
import com.popcorn.demo.domain.store.dto.StoreListDto;
import com.popcorn.demo.domain.store.dto.StoreStatusUpdatedDto;
import com.popcorn.demo.domain.store.dto.StoreUpdatedDto;
import com.popcorn.demo.domain.store.dto.UpdateStoreRequest;
import com.popcorn.demo.domain.store.dto.UpdateStoreStatusRequest;
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
        Long ownerId = 1001L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("통합테스트 팝콘 스토어")
                .ownerId(ownerId)
                .build();

        StoreCreatedDto result = storeService.createStore(ownerId, request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("통합테스트 팝콘 스토어");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getPublishStatus()).isEqualTo(StorePublishStatus.DRAFT);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getCreatedBy()).isEqualTo(ownerId);

        Store savedStore = storeRepository.findById(result.getId()).orElse(null);
        assertThat(savedStore).isNotNull();
        assertThat(savedStore.getName()).isEqualTo("통합테스트 팝콘 스토어");
        assertThat(savedStore.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("중복된 스토어 이름으로 생성 실패")
    void 중복된_스토어_이름으로_생성_실패() {
        Long firstOwnerId = 1001L;
        Long secondOwnerId = 1002L;
        String storeName = "중복테스트 스토어";

        CreateStoreRequest firstRequest = CreateStoreRequest.builder()
                .name(storeName)
                .ownerId(firstOwnerId)
                .build();
        storeService.createStore(firstOwnerId, firstRequest);

        CreateStoreRequest secondRequest = CreateStoreRequest.builder()
                .name(storeName)
                .ownerId(secondOwnerId)
                .build();

        assertThatThrownBy(() -> storeService.createStore(secondOwnerId, secondRequest))
                .isInstanceOf(StoreException.class);
    }

    @Test
    @DisplayName("스토어 기본 정보 수정 전체 플로우 성공")
    void 스토어_기본_정보_수정_전체_플로우_성공() {
        Long ownerId = 2001L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("수정 전 스토어")
                .ownerId(ownerId)
                .build();

        StoreCreatedDto created = storeService.createStore(ownerId, request);

        UpdateStoreRequest updateRequest = UpdateStoreRequest.builder()
                .name("수정 후 스토어")
                .build();

        StoreUpdatedDto updated = storeService.updateStore(created.getId(), updateRequest, ownerId);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("수정 후 스토어");
        assertThat(updated.getUpdatedBy()).isEqualTo(ownerId);

        Store stored = storeRepository.findById(created.getId()).orElse(null);
        assertThat(stored).isNotNull();
        assertThat(stored.getName()).isEqualTo("수정 후 스토어");
    }

    @Test
    @DisplayName("스토어 상태 수정 전체 플로우 성공")
    void 스토어_상태_수정_전체_플로우_성공() {
        Long ownerId = 3001L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("상태 수정 스토어")
                .ownerId(ownerId)
                .build();

        StoreCreatedDto created = storeService.createStore(ownerId, request);

        UpdateStoreStatusRequest statusRequest = UpdateStoreStatusRequest.builder()
                .publishStatus(StorePublishStatus.ACTIVE)
                .build();

        StoreStatusUpdatedDto updated = storeService.updateStoreStatus(created.getId(), statusRequest, ownerId);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
        assertThat(updated.getUpdatedBy()).isEqualTo(ownerId);

        Store stored = storeRepository.findById(created.getId()).orElse(null);
        assertThat(stored).isNotNull();
        assertThat(stored.getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
    }

    @Test
    @DisplayName("스토어 삭제 전체 플로우 성공")
    void 스토어_삭제_전체_플로우_성공() {
        Long ownerId = 4001L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("삭제 테스트 스토어")
                .ownerId(ownerId)
                .build();

        StoreCreatedDto created = storeService.createStore(ownerId, request);

        StoreDeletedDto deleted = storeService.deleteStore(created.getId(), ownerId);

        assertThat(deleted.getId()).isEqualTo(created.getId());
        assertThat(deleted.getDeletedBy()).isEqualTo(ownerId);
        assertThat(deleted.getDeletedAt()).isNotNull();

        Store stored = storeRepository.findById(created.getId()).orElse(null);
        assertThat(stored).isNotNull();
        assertThat(stored.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("내 스토어 목록 조회 시 삭제된 스토어 제외")
    void 내_스토어_목록_조회_삭제된_스토어_제외() {
        Long ownerId = 5001L;
        CreateStoreRequest firstRequest = CreateStoreRequest.builder()
                .name("목록 스토어 1")
                .ownerId(ownerId)
                .build();
        CreateStoreRequest secondRequest = CreateStoreRequest.builder()
                .name("목록 스토어 2")
                .ownerId(ownerId)
                .build();

        StoreCreatedDto first = storeService.createStore(ownerId, firstRequest);
        StoreCreatedDto second = storeService.createStore(ownerId, secondRequest);

        storeService.deleteStore(second.getId(), ownerId);

        java.util.List<StoreListDto> stores = storeService.getStoresByOwnerId(ownerId);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("스토어 상세 조회 전체 플로우 성공")
    void 스토어_상세_조회_전체_플로우_성공() {
        Long ownerId = 6001L;
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("상세 조회 스토어")
                .ownerId(ownerId)
                .build();

        StoreCreatedDto created = storeService.createStore(ownerId, request);

        StoreDetailDto detail = storeService.getStoreDetail(ownerId, created.getId());

        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getName()).isEqualTo("상세 조회 스토어");
        assertThat(detail.getOwnerId()).isEqualTo(ownerId);
    }
}
