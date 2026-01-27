package com.popcorn.store.domain.popup.service.manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.store.domain.popup.dto.manager.PendingStoreListResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreApproveResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreForceStopRequest;
import com.popcorn.store.domain.popup.dto.manager.StoreForceStopResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreRejectResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreWithdrawResponse;
import com.popcorn.store.domain.popup.entity.Popup;
import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.entity.enums.PopupStatus;
import com.popcorn.store.domain.popup.exception.PopupException;
import com.popcorn.store.domain.popup.exception.manager.ManagerPopupException;
import com.popcorn.store.domain.popup.repository.PopupQueryRepository;
import com.popcorn.store.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.store.domain.popup.repository.owner.OwnerPopupScheduleRepository;
import com.popcorn.store.domain.popup.repository.view.PopupListView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerPopupService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final OwnerPopupRepository ownerPopupRepository;
    private final OwnerPopupScheduleRepository ownerPopupScheduleRepository;
    private final PopupQueryRepository popupQueryRepository;

    @Transactional
    public StoreForceStopResponse forceStopPopup(UUID popupId, Long managerId, StoreForceStopRequest request) {
        Popup popup = findPopup(popupId);
        if (popup.getStatus() == PopupStatus.CANCELLED || popup.getStatus() == PopupStatus.CLOSED) {
            throw ManagerPopupException.popupAlreadyInactive();
        }
        if (popup.getStatus() != PopupStatus.APPROVED && popup.getStatus() != PopupStatus.OPEN) {
            throw ManagerPopupException.popupNotApproved();
        }

        Long resolvedManagerId = resolveManagerId(managerId);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            log.info("매니저(force_stop) reason={}", request.getReason());
        }
        Popup saved = applyStatusChange(popup, PopupStatus.CANCELLED, resolvedManagerId);
        return StoreForceStopResponse.builder()
                .popupId(saved.getId())
                .status(saved.getStatus())
                .updatedAt(saved.getUpdatedAt())
                .updatedBy(saved.getUpdatedBy())
                .build();
    }

    @Transactional
    public StoreApproveResponse approvePopup(UUID popupId, Long managerId) {
        Popup popup = findPopup(popupId);
        if (popup.getStatus() != PopupStatus.REQUEST) {
            throw ManagerPopupException.popupNotInRequest();
        }

        Long resolvedManagerId = resolveManagerId(managerId);
        Popup saved = applyStatusChange(popup, PopupStatus.APPROVED, resolvedManagerId);
        return StoreApproveResponse.builder()
                .popupId(saved.getId())
                .status(saved.getStatus())
                .updatedAt(saved.getUpdatedAt())
                .updatedBy(saved.getUpdatedBy())
                .build();
    }

    @Transactional
    public StoreRejectResponse rejectPopup(UUID popupId, Long managerId) {
        Popup popup = findPopup(popupId);
        if (popup.getStatus() != PopupStatus.REQUEST) {
            throw ManagerPopupException.popupNotInRequest();
        }

        Long resolvedManagerId = resolveManagerId(managerId);
        Popup saved = applyStatusChange(popup, PopupStatus.CANCELLED, resolvedManagerId);
        return StoreRejectResponse.builder()
                .popupId(saved.getId())
                .status(saved.getStatus())
                .updatedAt(saved.getUpdatedAt())
                .updatedBy(saved.getUpdatedBy())
                .build();
    }

    @Transactional
    public StoreWithdrawResponse withdrawPopup(UUID popupId, Long managerId) {
        Popup popup = findPopup(popupId);
        if (popup.getStatus() != PopupStatus.REQUEST) {
            throw ManagerPopupException.popupNotInRequest();
        }

        Long resolvedManagerId = resolveManagerId(managerId);
        Popup saved = applyStatusChange(popup, PopupStatus.CANCELLED, resolvedManagerId);
        return StoreWithdrawResponse.builder()
                .popupId(saved.getId())
                .status(saved.getStatus())
                .updatedAt(saved.getUpdatedAt())
                .updatedBy(saved.getUpdatedBy())
                .build();
    }

    @Transactional(readOnly = true)
    public PendingStoreListResponse getPendingPopups(Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        long offset = (long) (normalizedPage - 1) * normalizedSize;

        List<PopupListView> views = popupQueryRepository.findPopupsByStatus(
                PopupStatus.REQUEST.name(),
                normalizedSize,
                offset
        );
        long total = popupQueryRepository.countPopupsByStatus(PopupStatus.REQUEST.name());

        List<PendingStoreListResponse.ItemDto> items = views.stream()
                .map(view -> PendingStoreListResponse.ItemDto.builder()
                        .popupId(toUuid(view.getId()))
                        .storeId(toUuid(view.getStoreId()))
                        .title(view.getTitle())
                        .popupCategory(toCategory(view.getCategory()))
                        .status(toStatus(view.getStatus()))
                        .eventStartAt(view.getEventStartAt())
                        .eventEndAt(view.getEventEndAt())
                        .build())
                .toList();

        return PendingStoreListResponse.builder()
                .items(items)
                .page(normalizedPage)
                .size(normalizedSize)
                .total(total)
                .build();
    }

    private Popup findPopup(UUID popupId) {
        if (popupId == null) {
            throw new IllegalArgumentException("팝업 ID가 필요합니다.");
        }
        return ownerPopupRepository.findById(popupId)
                .orElseThrow(() -> PopupException.popupNotFound());
    }

    private Popup applyStatusChange(Popup popup, PopupStatus status, Long updatedBy) {
        popup.setStatus(status);
        popup.setUpdatedBy(updatedBy);

        Popup saved = ownerPopupRepository.save(popup);
        if (isInactiveStatus(status)) {
            ownerPopupScheduleRepository.deactivateActiveSchedulesByPopup(
                    saved.getId(),
                    LocalDateTime.now(),
                    updatedBy
            );
        }
        return saved;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private PopupCategory toCategory(String value) {
        return value == null ? null : PopupCategory.valueOf(value.toUpperCase());
    }

    private PopupStatus toStatus(String value) {
        if (value == null) {
            return null;
        }
        if ("CANCELED".equalsIgnoreCase(value)) {
            return PopupStatus.CANCELLED;
        }
        return PopupStatus.valueOf(value.toUpperCase());
    }

    private UUID toUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private boolean isInactiveStatus(PopupStatus status) {
        return status == PopupStatus.DRAFT || status == PopupStatus.CLOSED || status == PopupStatus.CANCELLED;
    }

    private Long resolveManagerId(Long managerId) {
        if (managerId == null) {
            throw ManagerPopupException.managerIdRequired();
        }
        return managerId;
    }
}
