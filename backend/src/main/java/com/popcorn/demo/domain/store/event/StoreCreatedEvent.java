package com.popcorn.demo.domain.store.event;

import com.popcorn.demo.domain.store.entity.Store;
import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreCreatedEvent {

    private final Long ownerId;
    private final Store store;

    public StoreCreatedEvent(Long ownerId, Store store) {
        this.ownerId = ownerId;
        this.store = store;
    }

}
