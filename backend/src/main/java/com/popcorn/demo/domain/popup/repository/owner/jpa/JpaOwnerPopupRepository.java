package com.popcorn.demo.domain.popup.repository.owner.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.popup.entity.Popup;

public interface JpaOwnerPopupRepository extends JpaRepository<Popup, UUID> {
}
