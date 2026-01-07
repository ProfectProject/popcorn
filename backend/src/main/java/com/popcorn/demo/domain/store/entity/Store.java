package com.popcorn.demo.domain.store.entity;

import com.popcorn.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;
// p_stores에 매핑되는 스토어 애그리게이트 루트.
@Entity
@Table(name = "p_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "store_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long ownerId;

    @Column(name = "store_name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "store_status")
    private StorePublishStatus publishStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
    public void updateName(String name) {
        this.name = name;
    }
    
    public void updatePublishStatus(StorePublishStatus status) {
        this.publishStatus = status;
    }

    public void delete(Long deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    // 접근 제어에 사용하는 오너 확인.
    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isActive() {
        return StorePublishStatus.ACTIVE.equals(this.publishStatus);
    }

    public boolean isDraft() {
        return StorePublishStatus.DRAFT.equals(this.publishStatus);
    }

}
