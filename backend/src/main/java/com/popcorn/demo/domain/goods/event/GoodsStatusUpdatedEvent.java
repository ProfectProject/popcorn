package com.popcorn.demo.domain.goods.event;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;

public record GoodsStatusUpdatedEvent(Long ownerId, GoodsVariant goods) {

}
