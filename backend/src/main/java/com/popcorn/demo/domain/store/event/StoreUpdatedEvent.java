package com.popcorn.demo.domain.store.event;

import com.popcorn.demo.domain.store.entity.Store;
import lombok.Getter;

@Getter
public class StoreUpdatedEvent {

	private final Long ownerId;
	private final Store store;

	public StoreUpdatedEvent(Long ownerId, Store store) {
		this.ownerId = ownerId;
		this.store = store;
	}
}
