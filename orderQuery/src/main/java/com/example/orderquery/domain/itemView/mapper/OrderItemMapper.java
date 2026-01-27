package com.example.orderquery.domain.itemView.mapper;

import com.example.orderquery.domain.itemView.dto.OrderItemDto;
import com.example.orderquery.domain.itemView.entity.OrderItemView;

public final class OrderItemMapper {

    private OrderItemMapper() {
    }

    public static OrderItemDto toDto(OrderItemView view) {
        return OrderItemDto.builder()
                .popupId(view.getId().getPopupId())
                .orderGoodsId(view.getId().getOrderGoodsId())
                .orderId(view.getOrderId())
                .storeId(view.getStoreId())
                .userId(view.getUserId())
                .orderNo(view.getOrderNo())
                .orderStatus(view.getOrderStatus() != null ? view.getOrderStatus().name() : null)
                .orderedAt(view.getOrderedAt())
                .itemType(view.getItemType())
                .scheduleId(view.getScheduleId())
                .scheduleStartAt(view.getScheduleStartAt())
                .scheduleEndAt(view.getScheduleEndAt())
                .goodsId(view.getGoodsId())
                .goodsName(view.getGoodsName())
                .stockUnit(view.getStockUnit())
                .qty(view.getQty())
                .unitPrice(view.getUnitPrice())
                .linePrice(view.getLinePrice())
                .paymentStatus(view.getPaymentStatus() != null ? view.getPaymentStatus().name() : null)
                .paymentApprovedAt(view.getPaymentApprovedAt())
                .checkedIn(view.isCheckedIn())
                .checkinAt(view.getCheckinAt())
                .build();
    }
}
