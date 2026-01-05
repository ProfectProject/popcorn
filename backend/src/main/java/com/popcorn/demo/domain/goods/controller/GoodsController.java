package com.popcorn.demo.domain.goods.controller;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.goods.dto.GoodsCreateRequest;
import com.popcorn.demo.domain.goods.dto.GoodsIdResponse;
import com.popcorn.demo.domain.goods.service.GoodsService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/owner/popups/{popupId}/goods")
public class GoodsController extends BaseController {
    private final GoodsService goodsService;

    @PostMapping
    public ResponseEntity<BaseResponse<GoodsIdResponse>> create(
            @PathVariable UUID popupId,
            @Valid @RequestBody GoodsCreateRequest request
    ) {
        return ok(goodsService.create(popupId, request));
    }
}
