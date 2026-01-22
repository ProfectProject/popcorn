package com.popcorn.store.domain.goods.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsStockResponse {

    private UUID goodsId;
    private int stock;
    private int reservationStock;

}
