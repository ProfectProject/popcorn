package com.popcorn.demo.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스토어 생성 요청 DTO
 * - API 요청으로 받는 스토어 생성 데이터
 * - Jakarta Validation 적용으로 입력 검증
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreRequest {

    // ========================= 기본 스토어 정보 =========================

    /**
     * 스토어 이름
     */
    @NotBlank(message = "스토어 이름은 필수입니다.")
    @Size(min = 1, max = 100, message = "스토어 이름은 1-100자 사이여야 합니다.")
    private String name;

    /**
     * 오너 ID
     */
    @NotNull(message = "오너 ID는 필수입니다.")
    @Positive(message = "오너 ID는 양수여야 합니다.")
    private Long ownerId;

    // ========================= 검증 메서드 =========================

    /**
     * 오너 권한 확인
     * @param userId 확인할 사용자 ID
     * @return 오너이면 true
     */
    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }

    /**
     * 요청 데이터 유효성 검증
     * @return 모든 필수 데이터가 유효하면 true
     */
    public boolean isValidRequest() {
        return name != null && !name.trim().isEmpty() 
            && ownerId != null && ownerId > 0;
    }

}
