package com.popcorn.demo.domain.store.entity;

import com.popcorn.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.UuidGenerator;

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
    @Column(name = "store_id", columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long ownerId;

    @Column(name = "store_name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StorePublishStatus publishStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    // Soft delete 필드 (BaseEntity에서 상속받은 deletedAt 사용)
    @Column(name = "deleted_by")
    private Long deletedBy;

    // Audit 필드 (BaseEntity에 추가 필요)
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
        super.delete(); // BaseEntity의 delete() 메서드 호출
        this.deletedBy = deletedBy;
    }

    // 접근 제어에 사용하는 오너 확인.
    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }

    public boolean isDeleted() {
        return super.isDeleted(); // BaseEntity의 isDeleted() 메서드 사용
    }

    public boolean isActive() {
        return StorePublishStatus.ACTIVE.equals(this.publishStatus);
    }

    public boolean isDraft() {
        return StorePublishStatus.DRAFT.equals(this.publishStatus);
    }

}
