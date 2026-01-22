package com.popcorn.store.domain.goods.dto;

import com.popcorn.store.domain.goods.entity.GoodsVariant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class GoodsItemResponse {
    @Schema(description = "굿즈 ID", example = "00000000-0000-0000-0000-000000000401")
    private UUID id;
    @Schema(description = "팝업 ID", example = "00000000-0000-0000-0000-000000000101")
    private UUID popupId;
    @Schema(description = "재고 단위", example = "개")
    private String stockUnit;
    @Schema(description = "굿즈 이름", example = "팝콘 키링")
    private String goodsName;
    @Schema(description = "굿즈 가격", example = "12000")
    private int goodsPrice;
    @Schema(description = "재고 수량", example = "100")
    private int stock;
    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;
    @Schema(description = "생성 시각", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "수정 시각", example = "2025-01-02T12:30:00")
    private LocalDateTime updatedAt;

    public static GoodsItemResponse fromOwner(GoodsVariant goods) {
        return GoodsItemResponse.builder()
                .id(goods.getId())
                .popupId(goods.getPopupId())
                .stockUnit(goods.getStockUnit())
                .goodsName(goods.getGoodsName())
                .goodsPrice(goods.getGoodsPrice())
                .stock(goods.getStock())
                .isActive(goods.isActive())
                .createdAt(goods.getCreatedAt())
                .updatedAt(goods.getUpdatedAt())
                .build();
    }

    public static GoodsItemResponse fromUser(GoodsVariant goods) {
        return GoodsItemResponse.builder()
                .goodsName(goods.getGoodsName())
                .goodsPrice(goods.getGoodsPrice())
                .stock(goods.getStock())
                .build();
    }
}
