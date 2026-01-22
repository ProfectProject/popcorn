package com.popcorn.demo.domain.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoodsCreateRequest {
    @NotBlank(message = "재고 단위는 필수입니다.")
    @Size(max = 64, message = "재고 단위는 최대 64자까지 입력할 수 있습니다.")
    @Schema(description = "재고 단위", example = "개")
    private String stockUnit;

    @NotBlank(message = "굿즈 이름은 필수입니다.")
    @Size(min = 1, max = 100, message = "굿즈 이름은 1-100자 사이여야 합니다.")
    @Schema(description = "굿즈 이름", example = "팝콘 키링")
    private String goodsName;

    @NotNull(message = "굿즈 가격은 필수입니다.")
    @Min(value = 0, message = "굿즈 가격은 0 이상이어야 합니다.")
    @Schema(description = "굿즈 가격", example = "12000")
    private Integer goodsPrice;

    @NotNull(message = "재고 수량은 필수입니다.")
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
    @Schema(description = "재고 수량", example = "100")
    private Integer stock;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;
}
