package com.popcorn.demo.domain.goods.event;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;

public record GoodsUpdatedEvent(Long ownerId, GoodsVariant goods) {

}
