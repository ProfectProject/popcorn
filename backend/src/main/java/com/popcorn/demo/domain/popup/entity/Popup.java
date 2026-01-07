package com.popcorn.demo.domain.popup.entity;

import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(name = "category")
	private String category;

	@Column(name = "status")
	private String status;

	@Column(name = "deleted_at")
	private java.time.LocalDateTime deletedAt;
}
