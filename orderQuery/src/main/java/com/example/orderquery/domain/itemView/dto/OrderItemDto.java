package com.example.orderquery.domain.itemView.dto;

import java.time.LocalDateTime;
import java.util.UUID;


import com.example.orderquery.domain.itemView.entity.ItemType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class OrderItemDto {

    private final UUID popupId;

    private final UUID orderGoodsId;
    private final UUID orderId;

    private final UUID storeId;
    private final Long userId;

    private final String orderNo;
    private final String orderStatus;
    private final LocalDateTime orderedAt;

    private final ItemType itemType;

    // schedule
    private final UUID scheduleId;
    private final LocalDateTime scheduleStartAt;
    private final LocalDateTime scheduleEndAt;

    // goods
    private final UUID goodsId;
    private final String goodsName;
    private final String stockUnit;

    private final int qty;
    private final int unitPrice;
    private final int linePrice;

    private final String paymentStatus;
    private final LocalDateTime paymentApprovedAt;

    private final boolean checkedIn;
    private final LocalDateTime checkinAt;
}