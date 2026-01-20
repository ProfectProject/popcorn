package com.popcorn.demo.domain.goods.entity;

import com.popcorn.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_goods_variants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsVariant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "goods_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "popup_id", nullable = false)
    private UUID popupId;

    @Column(name = "stock_unit", length = 64)
    private String stockUnit;

    @Column(name = "goods_name", nullable = false, length = 100)
    private String goodsName;

    @Column(name = "goods_price", nullable = false)
    private int goodsPrice;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static GoodsVariant create(
            UUID popupId,
            String stockUnit,
            String goodsName,
            int goodsPrice,
            int stock,
            boolean isActive
    ) {
        GoodsVariant goods = new GoodsVariant();
        goods.popupId = popupId;
        goods.stockUnit = stockUnit;
        goods.goodsName = goodsName;
        goods.goodsPrice = goodsPrice;
        goods.stock = stock;
        goods.isActive = isActive;
        return goods;
    }

    public void update(
            String goodsName,
            int goodsPrice,
            int stock
    ) {
        this.goodsName = goodsName;
        this.goodsPrice = goodsPrice;
        this.stock = stock;
    }

    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
