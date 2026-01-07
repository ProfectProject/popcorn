package com.popcorn.demo.domain.popup.repository.owner;

import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerPopupRepository {

    Popup save(Popup popup);
    Optional<Popup> findByStoreId(UUID storeId);
    void deleteById(UUID popupId);

    List<Popup> findAllByStoreId(UUID storeId);
    List<Popup> findAllByStoreIdAndDeletedAtIsNull(UUID storeId);
    List<Popup> findByPublishStatusAndStoreId(PopupStatus status, UUID storeId);
    List<Popup> findByPopupCategoryAndStoreId(PopupCategory category, UUID storeId);

    long countByStoreId(UUID storeId);
    long countByPopupStatus(PopupStatus status);
    long countByPopupCategory(PopupCategory category);

}
