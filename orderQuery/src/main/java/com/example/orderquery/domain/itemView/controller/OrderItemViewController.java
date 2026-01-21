package com.example.orderquery.domain.itemView.controller;

import com.example.orderquery.domain.itemView.dto.OrderItemPageDto;
import com.example.orderquery.domain.itemView.dto.OrderItemQuery;
import com.example.orderquery.domain.itemView.service.OrderItemViewService;
import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.entity.BaseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/owner/stores/{storeId}/popups/{popupId}/orders/items")
public class OrderItemViewController extends BaseController {

    private final OrderItemViewService orderItemViewService;

    @GetMapping()
    public ResponseEntity<BaseResponse<OrderItemPageDto>> getItems(@PathVariable UUID storeId,
                                                                   @PathVariable UUID popupId,
                                                                   @Valid @ModelAttribute OrderItemQuery query) {
        return ok(orderItemViewService.getItems(storeId, popupId, query));
    }
}
