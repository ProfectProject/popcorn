package com.popcorn.demo.domain.goods.service;

import com.popcorn.demo.domain.goods.dto.GoodsCreateRequest;
import com.popcorn.demo.domain.goods.dto.GoodsIdResponse;
import com.popcorn.demo.domain.goods.entity.GoodsVariant;
import com.popcorn.demo.domain.goods.exception.GoodsNotFoundException;
import com.popcorn.demo.domain.goods.repository.GoodsVariantRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoodsService {
    private final GoodsVariantRepository goodsVariantRepository;

    @Transactional
    public GoodsIdResponse create(UUID popupId, GoodsCreateRequest request) {
        boolean isActive = request.getIsActive() == null || request.getIsActive();
        GoodsVariant goods = GoodsVariant.create(
                popupId,
                request.getStockUnit(),
                request.getGoodsName(),
                request.getGoodsPrice(),
                request.getStock(),
                isActive
        );
        goodsVariantRepository.save(goods);
        return new GoodsIdResponse(goods.getId());
    }

    private GoodsVariant getGoods(UUID popupId, UUID goodsId) {
        return goodsVariantRepository
                .findByIdAndPopupIdAndDeletedAtIsNull(goodsId, popupId)
                .orElseThrow(GoodsNotFoundException::new);
    }
}
