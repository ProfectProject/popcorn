package com.popcorn.demo.domain.goods.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GoodsEventHandler {

	private static final Logger log = LoggerFactory.getLogger(GoodsEventHandler.class);

	@EventListener
	public void handleCreated(GoodsCreatedEvent event) {
		log.info("Goods created - ownerId: {}, goodsId: {}, popupId: {}",
				event.getOwnerId(), event.getGoods().getId(), event.getGoods().getPopupId());
	}

	@EventListener
	public void handleUpdated(GoodsUpdatedEvent event) {
		log.info("Goods updated - ownerId: {}, goodsId: {}, popupId: {}",
				event.getOwnerId(), event.getGoods().getId(), event.getGoods().getPopupId());
	}

	@EventListener
	public void handleStatusUpdated(GoodsStatusUpdatedEvent event) {
		log.info("Goods status updated - ownerId: {}, goodsId: {}, active: {}",
				event.getOwnerId(), event.getGoods().getId(), event.getGoods().isActive());
	}

	@EventListener
	public void handleDeleted(GoodsDeletedEvent event) {
		log.info("Goods deleted - ownerId: {}, goodsId: {}, popupId: {}",
				event.getOwnerId(), event.getGoods().getId(), event.getGoods().getPopupId());
	}
}
