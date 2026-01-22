package com.popcorn.store.domain.popup.entity;

import java.util.UUID;

import com.popcorn.common.entity.BaseEntity;
import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.entity.enums.PopupStatus;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "popups")
@Setter
public class Popup extends BaseEntity {

	@Id
	@UuidGenerator
	@Column(name = "popup_id")
	private UUID id;

	@Column(name = "store_id")
	private UUID storeId;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "category")
	private PopupCategory category;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status")
	private PopupStatus status;

	@Column(name = "reservation_open_at")
	private java.time.LocalDateTime reservationOpenAt;

	@Column(name = "address_road")
	private String addressRoad;

	@Column(name = "address_detail")
	private String addressDetail;

	@Builder
	public Popup(
			UUID storeId,
			String title,
			String description,
			PopupCategory category,
			PopupStatus status,
			java.time.LocalDateTime reservationOpenAt,
			String addressRoad,
			String addressDetail,
			Long createdBy
	) {
		this.storeId = storeId;
		this.title = title;
		this.description = description;
		this.category = category;
		this.status = status;
		this.reservationOpenAt = reservationOpenAt;
		this.addressRoad = addressRoad;
		this.addressDetail = addressDetail;
		setCreatedBy(createdBy);
	}

}
