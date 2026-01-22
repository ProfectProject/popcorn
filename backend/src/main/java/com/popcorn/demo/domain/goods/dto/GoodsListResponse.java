package com.popcorn.demo.domain.goods.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoodsListResponse {
    @Schema(description = "굿즈 목록")
    private List<GoodsItemResponse> items;
}
