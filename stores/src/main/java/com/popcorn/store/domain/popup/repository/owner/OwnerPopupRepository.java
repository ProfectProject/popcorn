package com.popcorn.store.domain.popup.repository.owner;

import com.popcorn.store.domain.popup.entity.Popup;
import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.entity.enums.PopupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerPopupRepository {

    Popup save(Popup popup);
    Optional<Popup> findById(UUID popupId);
    void deleteById(UUID popupId);

    List<Popup> findAllByStoreIdAndDeletedAtIsNull(UUID storeId);
    Optional<Popup> findOwnedPopup(UUID popupId, Long ownerId);
    List<Popup> findOwnedPopupsByStore(UUID storeId, Long ownerId);
    List<Popup> findOwnedPopupsByStoreWithPagination(UUID storeId, Long ownerId, int page, int size, String category);
    boolean existsOwnedStore(UUID storeId, Long ownerId);
    List<Popup> findByPublishStatusAndStoreId(PopupStatus status, UUID storeId);
    List<Popup> findByPopupCategoryAndStoreId(PopupCategory category, UUID storeId);

    long countByStoreId(UUID storeId);
    long countByPopupStatus(PopupStatus status);
    long countByPopupCategory(PopupCategory category);

}
