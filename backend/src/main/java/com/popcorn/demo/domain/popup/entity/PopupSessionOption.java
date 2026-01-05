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
@Table(name = "p_session_options")
public class PopupSessionOption extends BaseEntity {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "session_id")
	private UUID sessionId;

	@Column(name = "name")
	private String name;

	@Column(name = "price")
	private Integer price;

	@Column(name = "capacity")
	private Integer capacity;

	@Column(name = "is_hidden")
	private boolean hidden;
}
