package com.popcorn.demo.domain.goods.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoodsStatusResponse {
    private UUID id;
    private Boolean isActive;
}
