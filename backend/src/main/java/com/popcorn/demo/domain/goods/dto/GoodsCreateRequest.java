package com.popcorn.demo.domain.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoodsCreateRequest {
    @Schema(description = "재고 단위", example = "개")
    private String stockUnit;

    @NotBlank
    @Schema(description = "굿즈 이름", example = "팝콘 키링")
    private String goodsName;

    @NotNull
    @Min(0)
    @Schema(description = "굿즈 가격", example = "12000")
    private Integer goodsPrice;

    @NotNull
    @Min(0)
    @Schema(description = "재고 수량", example = "100")
    private Integer stock;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;
}
