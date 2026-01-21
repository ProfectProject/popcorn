package com.example.orderquery.domain.itemView.entity;

import jakarta.persistence.Column;

import java.io.Serializable;
import java.util.UUID;

public class OrderItemViewId implements Serializable {

    @Column(name = "popup_id", nullable = false)
    private UUID popupId;

    @Column(name = "order_goods_id", nullable = false)
    private UUID orderGoodsId;
}
