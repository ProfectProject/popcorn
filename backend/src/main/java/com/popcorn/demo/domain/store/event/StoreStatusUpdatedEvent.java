package com.popcorn.demo.domain.store.event;

import com.popcorn.demo.domain.store.entity.Store;

public record StoreStatusUpdatedEvent(Long ownerId, Store store) {

}
