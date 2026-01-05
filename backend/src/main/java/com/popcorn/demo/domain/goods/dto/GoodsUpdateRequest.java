package com.popcorn.demo.domain.merch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MerchUpdateRequest {
    private String sku;

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer price;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    private Boolean isHidden;
}
