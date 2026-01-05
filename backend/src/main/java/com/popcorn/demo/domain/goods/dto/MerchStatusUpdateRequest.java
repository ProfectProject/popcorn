package com.popcorn.demo.domain.merch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MerchStatusUpdateRequest {
    @NotNull
    private Boolean isHidden;
}
