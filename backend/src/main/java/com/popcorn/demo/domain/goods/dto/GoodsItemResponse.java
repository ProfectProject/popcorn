package com.popcorn.demo.domain.goods.dto;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
