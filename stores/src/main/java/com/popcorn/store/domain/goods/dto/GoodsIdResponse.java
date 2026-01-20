package com.popcorn.store.domain.goods.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoodsIdResponse {
    @Schema(description = "굿즈 ID", example = "00000000-0000-0000-0000-000000000401")
    private UUID id;
}
