package com.popcorn.demo.domain.goods.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoodsStatusUpdateRequest {
    @NotNull
    private Boolean isActive;
}
