package com.popcorn.demo.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@CreatedDate
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@CreatedBy
	@Column(name = "created_by")
	private Long createdBy;

	@LastModifiedBy
	@Column(name = "updated_by")
	private Long updatedBy;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "deleted_by")
	private Long deletedBy;

	/**
	 * 소프트 삭제 수행
	 */
	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	/**
	 * 삭제된 엔티티인지 확인
	 */
	public boolean isDeleted() {
		return deletedAt != null;
	}
}
