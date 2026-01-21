package com.popcorn.store.domain.popup.repository.owner.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.popcorn.store.domain.popup.entity.Popup;


public interface JpaOwnerPopupRepository extends JpaRepository<Popup, UUID> {

	@Query("select p from Popup p join Store s on s.id = p.storeId "
			+ "where p.id = :popupId and s.ownerId = :ownerId and s.deletedAt is null and p.deletedAt is null")
	Optional<Popup> findOwnedPopup(@Param("popupId") UUID popupId, @Param("ownerId") Long ownerId);

	@Query("select p from Popup p join Store s on s.id = p.storeId "
			+ "where p.storeId = :storeId and s.ownerId = :ownerId and s.deletedAt is null and p.deletedAt is null")
	List<Popup> findOwnedPopupsByStore(@Param("storeId") UUID storeId, @Param("ownerId") Long ownerId);

	@Query("select (count(s) > 0) from Store s where s.id = :storeId and s.ownerId = :ownerId and s.deletedAt is null")
	boolean existsOwnedStore(@Param("storeId") UUID storeId, @Param("ownerId") Long ownerId);
}
