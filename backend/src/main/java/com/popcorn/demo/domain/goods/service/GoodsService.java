package com.popcorn.demo.domain.goods.service;

import com.popcorn.demo.domain.goods.dto.GoodsCreateRequest;
import com.popcorn.demo.domain.goods.dto.GoodsIdResponse;
import com.popcorn.demo.domain.goods.dto.GoodsItemResponse;
import com.popcorn.demo.domain.goods.dto.GoodsListResponse;
import com.popcorn.demo.domain.goods.dto.GoodsUpdateRequest;
import com.popcorn.demo.domain.goods.entity.GoodsVariant;
import com.popcorn.demo.domain.goods.exception.GoodsNotFoundException;
import com.popcorn.demo.domain.goods.repository.GoodsVariantRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoodsService {
    private final GoodsVariantRepository goodsVariantRepository;

    @Transactional(readOnly = true)
    public GoodsListResponse list(UUID popupId) {
        List<GoodsItemResponse> items = goodsVariantRepository
                .findAllByPopupIdAndDeletedAtIsNullOrderByCreatedAtDesc(popupId)
                .stream()
                .map(GoodsItemResponse::from)
                .collect(Collectors.toList());
        return new GoodsListResponse(items);
    }

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

    @Transactional(readOnly = true)
    public GoodsItemResponse get(UUID popupId, UUID goodsId) {
        GoodsVariant goods = getGoods(popupId, goodsId);
        return GoodsItemResponse.from(goods);
    }

    @Transactional
    public GoodsIdResponse update(UUID popupId, UUID goodsId, GoodsUpdateRequest request) {
        GoodsVariant goods = getGoods(popupId, goodsId);
        goods.update(
                request.getStockUnit(),
                request.getGoodsName(),
                request.getGoodsPrice(),
                request.getStock(),
                request.getIsActive()
        );
        return new GoodsIdResponse(goods.getId());
    }

    private GoodsVariant getGoods(UUID popupId, UUID goodsId) {
        return goodsVariantRepository
                .findByIdAndPopupIdAndDeletedAtIsNull(goodsId, popupId)
                .orElseThrow(GoodsNotFoundException::new);
    }
}
