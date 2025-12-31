package com.popcorn.demo.domain.merch.controller;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.merch.dto.MerchListResponse;
import com.popcorn.demo.domain.merch.service.MerchService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/products/{productId}/merch")
public class MerchController extends BaseController {
    private final MerchService merchService;

    @GetMapping
    public ResponseEntity<BaseResponse<MerchListResponse>> list(@PathVariable UUID productId) {
        return ok(merchService.list(productId));
    }
}
