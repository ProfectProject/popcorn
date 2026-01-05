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
@Table(name = "p_product_sessions")
public class PopupSession extends BaseEntity {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "product_id")
	private UUID productId;

	@Column(name = "start_at")
	private LocalDateTime startAt;

	@Column(name = "end_at")
	private LocalDateTime endAt;

	@Column(name = "status")
	private String status;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}
