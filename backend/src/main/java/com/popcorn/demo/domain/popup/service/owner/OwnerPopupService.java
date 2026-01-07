package com.popcorn.demo.domain.popup.service.owner;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupCreatedDto;
import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.demo.domain.store.entity.Store;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerPopupService {

    private final OwnerPopupRepository ownerPopupRepository;
    private final StoreRepository storeRepository;
    private final OwnerPopupValidationService validationService;

    @Transactional
    public PopupCreatedDto createPopup(Long userId, CreatePopupRequest request) {
        log.info("[POPUP_CREATE] ownerId={}, storeId={}, title={}", userId, request.getStoreId(), request.getTitle());

        validateOwner(userId);
        String trimmedTitle = validationService.validateCreateRequest(request);

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> StoreException.storeNotFound(request.getStoreId()));

        if (!store.isOwner(userId)) {
            throw StoreException.accessDenied(userId, store.getId());
        }
        if (store.isDeleted()) {
            throw StoreException.storeAlreadyDeleted(store.getId());
        }

        validateDuplicateTitle(store.getId(), trimmedTitle);

        Popup savedPopup = ownerPopupRepository.save(createPopupEntity(userId, request, trimmedTitle));

        log.info("[POPUP_CREATED] popupId={}, storeId={}", savedPopup.getId(), savedPopup.getStoreId());
        return mapToDto(savedPopup);

    }

    private PopupCreatedDto mapToDto(Popup popup) {
        return PopupCreatedDto.builder()
                .popupId(popup.getId())
                .storeId(popup.getStoreId())
                .title(popup.getTitle())
                .description(popup.getDescription())
                .popupCategory(popup.getCategory())
                .status(popup.getStatus())
                .createdAt(popup.getCreatedAt())
                .createdBy(popup.getCreatedBy())
                .build();
    }


    private Popup createPopupEntity(Long userId, CreatePopupRequest request, String title) {
        return Popup.builder()
                .storeId(request.getStoreId())
                .title(title)
                .description(request.getDescription())
                .status(PopupStatus.DRAFT)
                .category(request.getCategory())
                .createdBy(userId)
                .build();
    }

    private void validateOwner(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw StoreException.ownerNotFound();
        }
    }

    private void validateDuplicateTitle(UUID storeId, String title) {
        List<Popup> popups = ownerPopupRepository.findAllByStoreIdAndDeletedAtIsNull(storeId);
        boolean exists = popups.stream()
                .filter(popup -> popup.getTitle() != null)
                .anyMatch(popup -> popup.getTitle().equals(title));
        if (exists) {
            throw new PopupException(PopupResponseCode.INVALID_REQUEST);
        }
    }

}
