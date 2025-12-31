package com.popcorn.demo.domain.store.entity;

import com.popcorn.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 스토어 엔티티
 * BaseEntity를 상속받아 표준화된 감사 필드를 포함합니다.
 * 
 * 매핑 테이블 : p_stores
 * 
 * 주요 기능 :
 * - 스토어 통합 관리
 * - 스토어 상태 변화 관리
 */
@Entity
@Table(name = "p_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private StorePublishStatus publishStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;

    // =================== 비즈니스 메서드 구현 =====================
    /**
     * 스토어 이름 변경
     */
    public void updateName(String name) {
        this.name = name;
    }
    
    /**
     * 스토어 상태 변경
     */
    public void updatePublishStatus(StorePublishStatus status) {
        this.publishStatus = status;
    }
    
    /**
     * 스토어 삭제(soft delete)
     */
    public void delete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    
    /**
     * 스토어의 오너인지 확인
     */
    public boolean isOwner(UUID userId) {
        return this.ownerId.equals(userId);
    }
    
    /**
     * 스토어가 삭제되었는지 확인
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}