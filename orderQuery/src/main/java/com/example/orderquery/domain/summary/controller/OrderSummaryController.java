package com.example.orderquery.domain.summary.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.orderquery.domain.itemView.service.OrderItemViewService;
import com.example.orderquery.domain.summary.dto.OrderSummaryDto;
import com.example.orderquery.domain.summary.service.OrderSummaryService;
import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import org.springframework.http.ResponseEntity;


import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/owner/stores/{storeId}/popups/{popupId}/orders/summary")
public class OrderSummaryController extends BaseController {

    private final OrderSummaryService orderSummaryService;
    @GetMapping()
    public ResponseEntity<BaseResponse<OrderSummaryDto>> getSummary(@PathVariable UUID storeId,
                                                                    @PathVariable UUID popupId) {
        return ok(orderSummaryService.getSummary(storeId, popupId));
    }
}
