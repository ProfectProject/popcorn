package com.popcorn.demo.domain.goods.event;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;

public record GoodsDeletedEvent(Long ownerId, GoodsVariant goods) {

}
