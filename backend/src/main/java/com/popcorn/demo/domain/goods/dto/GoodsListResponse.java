package com.popcorn.demo.domain.goods.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoodsListResponse {
    private List<GoodsItemResponse> items;
}
