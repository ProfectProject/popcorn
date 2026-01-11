package com.popcorn.demo.domain.popup.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.domain.popup.dto.owner.response.PopupListDto;
import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.demo.domain.popup.service.owner.OwnerPopupService;
import com.popcorn.demo.domain.popup.service.owner.OwnerPopupValidationService;

@ExtendWith(MockitoExtension.class)
class OwnerPopupServicePaginationTest {

    @Mock
    private OwnerPopupRepository ownerPopupRepository;

    @Mock
    private OwnerPopupValidationService validationService;

    @InjectMocks
    private OwnerPopupService ownerPopupService;

    private UUID storeId;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        ownerId = 1L;
    }

    @Test
    @DisplayName("페이지네이션 테스트 - 첫 번째 페이지")
    void 페이지네이션_첫번째_페이지_테스트() {
        // Given
        List<Popup> mockPopups = createMockPopups(5);
        when(ownerPopupRepository.findOwnedPopupsByStoreWithPagination(storeId, ownerId, 1, 2, null))
                .thenReturn(mockPopups.subList(0, 2));

        // When
        List<PopupListDto> result = ownerPopupService.getPopupByStoreId(ownerId, storeId, 1, 2, null);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("카테고리 필터링 테스트")
    void 카테고리_필터링_테스트() {
        // Given
        List<Popup> foodPopups = createMockPopupsWithCategory(PopupCategory.FOOD, 3);
        when(ownerPopupRepository.findOwnedPopupsByStoreWithPagination(storeId, ownerId, 1, 10, "FOOD"))
                .thenReturn(foodPopups);

        // When
        List<PopupListDto> result = ownerPopupService.getPopupByStoreId(ownerId, storeId, 1, 10, "FOOD");

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(popup -> popup.getPopupCategory() == PopupCategory.FOOD);
    }

    @Test
    @DisplayName("빈 페이지 테스트")
    void 빈_페이지_테스트() {
        // Given
        when(ownerPopupRepository.findOwnedPopupsByStoreWithPagination(storeId, ownerId, 1, 10, null))
                .thenReturn(List.of());

        // When
        List<PopupListDto> result = ownerPopupService.getPopupByStoreId(ownerId, storeId, 1, 10, null);

        // Then
        assertThat(result).isEmpty();
    }

    private List<Popup> createMockPopups(int count) {
        return List.of(
                createPopup("팝업1", PopupCategory.FOOD),
                createPopup("팝업2", PopupCategory.FASHION),
                createPopup("팝업3", PopupCategory.BEAUTY),
                createPopup("팝업4", PopupCategory.FOOD),
                createPopup("팝업5", PopupCategory.TECH)
        ).subList(0, Math.min(count, 5));
    }

    private List<Popup> createMockPopupsWithCategory(PopupCategory category, int count) {
        return List.of(
                createPopup("푸드팝업1", category),
                createPopup("푸드팝업2", category),
                createPopup("푸드팝업3", category)
        ).subList(0, Math.min(count, 3));
    }

    private Popup createPopup(String title, PopupCategory category) {
        return Popup.builder()
                .storeId(storeId)
                .title(title)
                .description("설명")
                .category(category)
                .status(PopupStatus.OPEN)
                .createdBy(ownerId)
                .build();
    }
}