package com.popcorn.demo.domain.store.event;

import com.popcorn.demo.domain.store.entity.Store;
import lombok.Getter;

@Getter
public class StoreDeletedEvent {

	private final Long ownerId;
	private final Store store;

	public StoreDeletedEvent(Long ownerId, Store store) {
		this.ownerId = ownerId;
		this.store = store;
	}
}
