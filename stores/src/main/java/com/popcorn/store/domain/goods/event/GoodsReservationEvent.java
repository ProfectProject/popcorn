package com.popcorn.store.domain.goods.event;

import java.util.UUID;

import com.popcorn.store.domain.goods.dto.GoodsStockResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoodsReservationEvent {

	public enum Action {
		RESERVE,
		CANCEL,
		FAIL,
		COMPLETE
	}

	private final Action action;
	private final UUID goodsId;
	private final int quantity;
	private GoodsStockResponse result;

	public GoodsReservationEvent(Action action, UUID goodsId, int quantity) {
		this.action = action;
		this.goodsId = goodsId;
		this.quantity = quantity;
	}
}
