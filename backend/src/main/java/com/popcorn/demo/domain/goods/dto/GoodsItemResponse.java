package com.popcorn.demo.domain.goods.dto;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoodsItemResponse {
    private UUID id;
    private UUID popupId;
    private String stockUnit;
    private String goodsName;
    private int goodsPrice;
    private int stock;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GoodsItemResponse from(GoodsVariant goods) {
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
}
