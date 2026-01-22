package com.popcorn.demo.domain.goods.event;

import com.popcorn.demo.domain.goods.entity.GoodsVariant;
import lombok.Getter;

@Getter
public class GoodsCreatedEvent {

	private final Long ownerId;
	private final GoodsVariant goods;

	public GoodsCreatedEvent(Long ownerId, GoodsVariant goods) {
		this.ownerId = ownerId;
		this.goods = goods;
	}
}
