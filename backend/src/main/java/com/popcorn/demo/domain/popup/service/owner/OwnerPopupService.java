package com.popcorn.demo.domain.popup.service.owner;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupStatusRequest;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDetailDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupCreatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupListDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupStatusUpdatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupUpdatedDto;
import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.demo.domain.store.exception.StoreException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerPopupService {

    private final OwnerPopupRepository ownerPopupRepository;
    private final OwnerPopupValidationService validationService;

    @Transactional
    public PopupCreatedDto createPopup(Long userId, CreatePopupRequest request) {
        log.info("[POPUP_CREATE] ownerId={}, storeId={}, title={}", userId, request.getStoreId(), request.getTitle());

        validateOwner(userId);
        String trimmedTitle = validationService.validateCreateRequest(request);

        if (!ownerPopupRepository.existsOwnedStore(request.getStoreId(), userId)) {
            throw StoreException.storeNotFound(request.getStoreId());
        }

        validateDuplicateTitle(request.getStoreId(), trimmedTitle);

        Popup savedPopup = ownerPopupRepository.save(createPopupEntity(userId, request, trimmedTitle));

        log.info("[POPUP_CREATED] popupId={}, storeId={}", savedPopup.getId(), savedPopup.getStoreId());
        return mapToDto(savedPopup);

    }

    @Transactional(readOnly = true)
    public List<PopupListDto> getPopupByStoreId(Long ownerId, UUID storeId) {
        log.info("[POPUP_LIST] ownerId={}, storeId={}", ownerId, storeId);

        validateOwner(ownerId);
        validateStoreId(storeId);

        List<PopupListDto> result = ownerPopupRepository.findOwnedPopupsByStore(storeId, ownerId).stream()
                .map(this::mapToListDto)
                .toList();

        log.info("[POPUP_LIST_FOUND] storeId={}, count={}", storeId, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public PopupDetailDto getPopupDetail(Long ownerId, UUID popupId) {
        log.info("[POPUP_DETAIL] ownerId={}, popupId={}", ownerId, popupId);

        validateOwner(ownerId);
        validatePopupId(popupId);

        Popup popup = ownerPopupRepository.findOwnedPopup(popupId, ownerId)
                .orElseThrow(PopupException::popupNotFound);

        PopupDetailDto detail = mapToDetailDto(popup);
        log.info("[POPUP_DETAIL_FOUND] popupId={}, storeId={}", popup.getId(), popup.getStoreId());
        return detail;
    }

    @Transactional
    public PopupUpdatedDto updatePopup(Long ownerId, UUID popupId, UpdatePopupRequest request) {
        log.info("[POPUP_UPDATE] ownerId={}, popupId={}", ownerId, popupId);

        validateOwner(ownerId);
        validatePopupId(popupId);

        String trimmedTitle = validationService.validateUpdateRequest(request);

        Popup popup = ownerPopupRepository.findOwnedPopup(popupId, ownerId)
                .orElseThrow(PopupException::popupNotFound);

        if (trimmedTitle != null) {
            validateDuplicateTitle(popup.getStoreId(), popup.getId(), trimmedTitle);
            popup.setTitle(trimmedTitle);
        }
        if (request.getDescription() != null) {
            popup.setDescription(request.getDescription());
        }
        if (request.getPopupCategory() != null) {
            popup.setCategory(request.getPopupCategory());
        }
        popup.setUpdatedBy(ownerId);

        Popup updatedPopup = ownerPopupRepository.save(popup);
        log.info("[POPUP_UPDATED] popupId={}, storeId={}", updatedPopup.getId(), updatedPopup.getStoreId());
        return mapToUpdatedDto(updatedPopup);
    }

    @Transactional
    public PopupStatusUpdatedDto updatePopupStatus(Long ownerId, UUID popupId, UpdatePopupStatusRequest request) {
        log.info("[POPUP_STATUS_UPDATE] ownerId={}, popupId={}, status={}", ownerId, popupId, request.getStatus());

        validateOwner(ownerId);
        validatePopupId(popupId);
        validateStatusRequest(request);

        Popup popup = ownerPopupRepository.findOwnedPopup(popupId, ownerId)
                .orElseThrow(PopupException::popupNotFound);

        popup.setStatus(request.getStatus());
        popup.setUpdatedBy(ownerId);

        Popup updatedPopup = ownerPopupRepository.save(popup);
        log.info("[POPUP_STATUS_UPDATED] popupId={}, status={}", updatedPopup.getId(), updatedPopup.getStatus());
        return mapToStatusUpdatedDto(updatedPopup);
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

    private PopupListDto mapToListDto(Popup popup) {
        return PopupListDto.builder()
                .popupId(popup.getId())
                .title(popup.getTitle())
                .popupCategory(popup.getCategory())
                .status(popup.getStatus())
                .createdAt(popup.getCreatedAt())
                .build();
    }

    private PopupDetailDto mapToDetailDto(Popup popup) {
        return PopupDetailDto.builder()
                .popupId(popup.getId())
                .storeId(popup.getStoreId())
                .title(popup.getTitle())
                .description(popup.getDescription())
                .popupCategory(popup.getCategory())
                .status(popup.getStatus())
                .createdAt(popup.getCreatedAt())
                .updatedAt(popup.getUpdatedAt())
                .build();
    }

    private PopupUpdatedDto mapToUpdatedDto(Popup popup) {
        return PopupUpdatedDto.builder()
                .popupId(popup.getId())
                .title(popup.getTitle())
                .description(popup.getDescription())
                .status(popup.getStatus())
                .popupCategory(popup.getCategory())
                .updatedBy(popup.getUpdatedBy())
                .updatedAt(popup.getUpdatedAt())
                .build();
    }

    private PopupStatusUpdatedDto mapToStatusUpdatedDto(Popup popup) {
        return PopupStatusUpdatedDto.builder()
                .popupId(popup.getId())
                .title(popup.getTitle())
                .status(popup.getStatus())
                .updatedBy(popup.getUpdatedBy())
                .updatedAt(popup.getUpdatedAt())
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

    private void validateStoreId(UUID storeId) {
        if (storeId == null) {
            throw new PopupException(PopupResponseCode.INVALID_REQUEST);
        }
    }

    private void validatePopupId(UUID popupId) {
        if (popupId == null) {
            throw new PopupException(PopupResponseCode.INVALID_REQUEST);
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

    private void validateDuplicateTitle(UUID storeId, UUID popupId, String title) {
        List<Popup> popups = ownerPopupRepository.findAllByStoreIdAndDeletedAtIsNull(storeId);
        boolean exists = popups.stream()
                .filter(popup -> popup.getTitle() != null)
                .filter(popup -> !popup.getId().equals(popupId))
                .anyMatch(popup -> popup.getTitle().equals(title));
        if (exists) {
            throw new PopupException(PopupResponseCode.INVALID_REQUEST);
        }
    }

    private void validateStatusRequest(UpdatePopupStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new PopupException(PopupResponseCode.INVALID_REQUEST);
        }
    }


}
