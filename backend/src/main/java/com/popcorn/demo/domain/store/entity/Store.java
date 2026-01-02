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
 * 매핑 테이블: p_stores
 * 
 * 주요 기능:
 * - 스토어 통합 관리
 * - 스토어 상태 변화 관리
 * - UUID 기반 식별자 사용
 */
@Entity
@Table(name = "p_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {

    // ========================= 기본 필드 =========================

    /** 스토어 ID (Primary Key) */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** 오너 ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 스토어 이름 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 발행 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private StorePublishStatus publishStatus;

    /** 낙관적 락 버전 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ========================= Soft Delete 필드 =========================

    /** 삭제 시간 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /** 삭제자 ID */
    @Column(name = "deleted_by")
    private Long deletedBy;

    // ========================= 감사 필드 =========================

    /** 생성자 ID */
    @Column(name = "created_by")
    private Long createdBy;
    
    /** 수정자 ID */
    @Column(name = "updated_by")
    private Long updatedBy;

    // BaseEntity에서 상속받는 필드들:
    // - createdAt: 생성 시간
    // - updatedAt: 수정 시간

    // ========================= 비즈니스 메서드 =========================

    /**
     * 스토어 이름 변경
     * @param name 새로운 스토어 이름
     */
    public void updateName(String name) {
        this.name = name;
    }
    
    /**
     * 스토어 상태 변경
     * @param status 새로운 발행 상태
     */
    public void updatePublishStatus(StorePublishStatus status) {
        this.publishStatus = status;
    }
    
    /**
     * 스토어 삭제 (Soft Delete)
     * @param deletedBy 삭제자 ID
     */
    public void delete(Long deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    
    /**
     * 스토어의 오너인지 확인
     * @param userId 확인할 사용자 ID
     * @return 오너이면 true
     */
    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }
    
    /**
     * 스토어가 삭제되었는지 확인
     * @return 삭제되었으면 true
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 스토어가 공개 상태인지 확인
     * @return 공개 상태이면 true
     */
    public boolean isPublished() {
        return StorePublishStatus.PUBLISHED.equals(this.publishStatus);
    }

    /**
     * 스토어가 임시저장 상태인지 확인
     * @return 임시저장 상태이면 true
     */
    public boolean isDraft() {
        return StorePublishStatus.DRAFT.equals(this.publishStatus);
    }

}