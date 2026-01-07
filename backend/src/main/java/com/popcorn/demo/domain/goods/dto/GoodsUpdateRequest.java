package com.popcorn.demo.domain.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoodsUpdateRequest {
    @NotBlank
    private String goodsName;

    @NotNull
    @Min(0)
    private Integer goodsPrice;

    @NotNull
    @Min(0)
    private Integer stock;

}
