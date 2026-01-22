package com.popcorn.store.domain.goods.event;

import com.popcorn.store.domain.goods.entity.GoodsVariant;
import lombok.Getter;

@Getter
public class GoodsStatusUpdatedEvent {

	private final Long ownerId;
	private final GoodsVariant goods;

	public GoodsStatusUpdatedEvent(Long ownerId, GoodsVariant goods) {
		this.ownerId = ownerId;
		this.goods = goods;
	}
}
