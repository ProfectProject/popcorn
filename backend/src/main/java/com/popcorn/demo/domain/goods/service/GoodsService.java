package com.popcorn.demo.domain.goods.service;

import com.popcorn.demo.domain.goods.dto.GoodsCreateRequest;
import com.popcorn.demo.domain.goods.dto.GoodsIdResponse;
import com.popcorn.demo.domain.goods.dto.GoodsItemResponse;
import com.popcorn.demo.domain.goods.dto.GoodsListResponse;
import com.popcorn.demo.domain.goods.dto.GoodsStatusResponse;
import com.popcorn.demo.domain.goods.dto.GoodsStatusUpdateRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class GoodsService {
    // TODO(ops-bc): goods bounded context 경계/공통 모듈 정의 (GoodsStatus, GoodsId, 공통 응답/에러 규격).
    // TODO(ops-event): GoodsCreated/Updated/Deleted/StatusChanged 이벤트 클래스 추가.
    // TODO(ops-event): 재고/상태 변경 시 이벤트 발행하고 주문 가능 여부/알림/통계 리스너 분리.
    private final GoodsVariantRepository goodsVariantRepository;

    @Transactional(readOnly = true)
    public GoodsListResponse list(UUID popupId) {
        boolean ownerView = isOwnerOrManager();
        List<GoodsItemResponse> items = goodsVariantRepository
                .findAllByPopupIdAndDeletedAtIsNullOrderByCreatedAtDesc(popupId)
                .stream()
                .map(goods -> ownerView ? GoodsItemResponse.fromOwner(goods) : GoodsItemResponse.fromUser(goods))
                .collect(Collectors.toList());
        return new GoodsListResponse(items);
    }

    @Transactional
    public GoodsIdResponse create(UUID popupId, GoodsCreateRequest request) {
        boolean isActive = Boolean.TRUE.equals(request.getIsActive());
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
        return isOwnerOrManager() ? GoodsItemResponse.fromOwner(goods) : GoodsItemResponse.fromUser(goods);
    }

    @Transactional
    public GoodsIdResponse update(UUID popupId, UUID goodsId, GoodsUpdateRequest request) {
        GoodsVariant goods = getGoods(popupId, goodsId);
        goods.update(
                request.getGoodsName(),
                request.getGoodsPrice(),
                request.getStock()
        );
        return new GoodsIdResponse(goods.getId());
    }

    @Transactional
    public GoodsStatusResponse updateStatus(
            UUID popupId,
            UUID goodsId,
            GoodsStatusUpdateRequest request
    ) {
        GoodsVariant goods = getGoods(popupId, goodsId);
        goods.updateStatus(request.getIsActive());
        return new GoodsStatusResponse(goods.getId(), goods.isActive());
    }

    @Transactional
    public void delete(UUID popupId, UUID goodsId) {
        GoodsVariant goods = getGoods(popupId, goodsId);
        goods.softDelete();
    }

    private GoodsVariant getGoods(UUID popupId, UUID goodsId) {
        return goodsVariantRepository
                .findByIdAndPopupIdAndDeletedAtIsNull(goodsId, popupId)
                .orElseThrow(GoodsNotFoundException::new);
    }

    private boolean isOwnerOrManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority())
                        || "ROLE_MANAGER".equals(authority.getAuthority()));
    }
}
