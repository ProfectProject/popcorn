package com.popcorn.demo.domain.popup.repository.owner.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;

@Repository
public class OwnerPopupRepositoryImpl implements OwnerPopupRepository {

	private final JpaOwnerPopupRepository jpaOwnerPopupRepository;

	public OwnerPopupRepositoryImpl(JpaOwnerPopupRepository jpaOwnerPopupRepository) {
		this.jpaOwnerPopupRepository = jpaOwnerPopupRepository;
	}

	@Override
	public Popup save(Popup popup) {
		return jpaOwnerPopupRepository.save(popup);
	}

	@Override
	public Optional<Popup> findById(UUID popupId) {
		return jpaOwnerPopupRepository.findById(popupId);
	}

	@Override
	public void deleteById(UUID popupId) {
		jpaOwnerPopupRepository.deleteById(popupId);
	}

	@Override
	public List<Popup> findAllByStoreIdAndDeletedAtIsNull(UUID storeId) {
		return jpaOwnerPopupRepository.findAll().stream()
				.filter(popup -> popup.getStoreId().equals(storeId) && popup.getDeletedAt() == null)
				.toList();
	}

	@Override
	public Optional<Popup> findOwnedPopup(UUID popupId, Long ownerId) {
		return jpaOwnerPopupRepository.findOwnedPopup(popupId, ownerId);
	}

	@Override
	public List<Popup> findOwnedPopupsByStore(UUID storeId, Long ownerId) {
		return jpaOwnerPopupRepository.findOwnedPopupsByStore(storeId, ownerId);
	}

	@Override
	public boolean existsOwnedStore(UUID storeId, Long ownerId) {
		return jpaOwnerPopupRepository.existsOwnedStore(storeId, ownerId);
	}

	@Override
	public List<Popup> findByPublishStatusAndStoreId(PopupStatus status, UUID storeId) {
		return jpaOwnerPopupRepository.findAll().stream()
				.filter(popup -> popup.getStoreId().equals(storeId) && popup.getStatus().equals(status))
				.toList();
	}

	@Override
	public List<Popup> findByPopupCategoryAndStoreId(PopupCategory category, UUID storeId) {
		return jpaOwnerPopupRepository.findAll().stream()
				.filter(popup -> popup.getStoreId().equals(storeId) && popup.getCategory().equals(category))
				.toList();
	}

	@Override
	public long countByStoreId(UUID storeId) {
		return jpaOwnerPopupRepository.findAll().stream()
				.filter(popup -> popup.getStoreId().equals(storeId))
				.count();
	}

	@Override
	public long countByPopupStatus(PopupStatus status) {
		return jpaOwnerPopupRepository.findAll().stream()
				.filter(popup -> popup.getStatus().equals(status))
				.count();
	}

	@Override
	public long countByPopupCategory(PopupCategory category) {
		return jpaOwnerPopupRepository.findAll().stream()
				.filter(popup -> popup.getCategory().equals(category))
				.count();
	}
}
