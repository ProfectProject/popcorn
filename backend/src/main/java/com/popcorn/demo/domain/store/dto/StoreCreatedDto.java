package com.popcorn.demo.domain.store.dto;

import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 스토어 생성 응답 DTO
 * - 스토어 생성 후 반환되는 데이터
 * - UUID 기반 ID 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreCreatedDto {

    /** 스토어 ID (UUID) */
    private UUID id;

    /** 스토어 이름 */
    private String name;

    /** 오너 ID */
    private Long ownerId;

    /** 발행 상태 */
    private StorePublishStatus publishStatus;

    /** 생성 시간 */
    private LocalDateTime createdAt;

    /** 생성자 ID */
    private Long createdBy;

}
