package com.popcorn.demo.domain.goods.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoodsStatusUpdateRequest {
    @NotNull(message = "활성화 여부는 필수입니다.")
    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;
}
