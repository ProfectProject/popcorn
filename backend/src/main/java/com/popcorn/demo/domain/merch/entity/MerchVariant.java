package com.popcorn.demo.domain.merch.entity;

import com.popcorn.demo.common.entity.BaseEntity;
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
@Table(name = "p_merch_variants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchVariant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static MerchVariant create(
            UUID productId,
            String sku,
            String name,
            int price,
            int stock,
            boolean isHidden
    ) {
        MerchVariant merch = new MerchVariant();
        merch.productId = productId;
        merch.sku = sku;
        merch.name = name;
        merch.price = price;
        merch.stock = stock;
        merch.isHidden = isHidden;
        return merch;
    }

    public void update(
            String sku,
            String name,
            int price,
            int stock,
            boolean isHidden
    ) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.isHidden = isHidden;
    }

    public void updateStatus(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
