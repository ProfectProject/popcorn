package com.popcorn.demo.domain.popup.entity;

import java.time.LocalDateTime;
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
@Table(name = "p_popup_schedules")
public class PopupSession extends BaseEntity {

	@Id
	@Column(name = "schedule_id")
	private UUID id;

	@Column(name = "popup_id")
	private UUID popupId;

	@Column(name = "start_at")
	private LocalDateTime startAt;

	@Column(name = "end_at")
	private LocalDateTime endAt;

	@Column(name = "price")
	private Integer price;

	@Column(name = "capacity")
	private Integer capacity;

	@Column(name = "remaining_capacity")
	private Integer remainingCapacity;

	@Column(name = "is_active")
	private boolean active;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}
