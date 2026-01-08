package com.popcorn.demo.domain.popup.entity;

import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_popups")
public class Popup extends BaseEntity {

	@Id
	@Column(name = "popup_id")
	private UUID id;

	@Column(name = "store_id")
	private UUID storeId;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private PopupCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private PopupStatus status;

	@Column(name = "deleted_at")
	private java.time.LocalDateTime deletedAt;
}
