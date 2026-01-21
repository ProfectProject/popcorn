package com.popcorn.store.domain.store.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StoreEventHandler {

	private static final Logger log = LoggerFactory.getLogger(StoreEventHandler.class);

	@EventListener
	public void handleUpdated(StoreUpdatedEvent event) {
		log.info("🛠️ 스토어 수정 이벤트 - ownerId: {}, storeId: {}, name: {}",
				event.getOwnerId(), event.getStore().getId(), event.getStore().getName());
	}

	@EventListener
	public void handleStatusUpdated(StoreStatusUpdatedEvent event) {
		log.info("📢 스토어 상태 변경 이벤트 - ownerId: {}, storeId: {}, status: {}",
				event.getOwnerId(), event.getStore().getId(), event.getStore().getPublishStatus());
	}

	@EventListener
	public void handleDeleted(StoreDeletedEvent event) {
		log.info("🗑️ 스토어 삭제 이벤트 - ownerId: {}, storeId: {}",
				event.getOwnerId(), event.getStore().getId());
	}

}
