package com.popcorn.store.domain.store.event;

import com.popcorn.store.domain.store.entity.Store;
import lombok.Getter;

@Getter
public class StoreStatusUpdatedEvent {

	private final Long ownerId;
	private final Store store;

	public StoreStatusUpdatedEvent(Long ownerId, Store store) {
		this.ownerId = ownerId;
		this.store = store;
	}
}
