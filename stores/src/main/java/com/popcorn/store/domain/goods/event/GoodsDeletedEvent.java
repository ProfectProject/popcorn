package com.popcorn.store.domain.goods.event;

import com.popcorn.store.domain.goods.entity.GoodsVariant;
import lombok.Getter;

@Getter
public class GoodsDeletedEvent {

	private final Long ownerId;
	private final GoodsVariant goods;

	public GoodsDeletedEvent(Long ownerId, GoodsVariant goods) {
		this.ownerId = ownerId;
		this.goods = goods;
	}
}
