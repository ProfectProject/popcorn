package com.popcorn.demo.domain.merch.dto;

import com.popcorn.demo.domain.merch.entity.MerchVariant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MerchItemResponse {
    private UUID id;
    private UUID productId;
    private String sku;
    private String name;
    private int price;
    private int stock;
    private Boolean isHidden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MerchItemResponse from(MerchVariant merch) {
        return MerchItemResponse.builder()
                .id(merch.getId())
                .productId(merch.getProductId())
                .sku(merch.getSku())
                .name(merch.getName())
                .price(merch.getPrice())
                .stock(merch.getStock())
                .isHidden(merch.isHidden())
                .createdAt(merch.getCreatedAt())
                .updatedAt(merch.getUpdatedAt())
                .build();
    }
}
