package com.popcorn.demo.domain.store.event;

import com.popcorn.demo.domain.store.entity.Store;

public record StoreCreatedEvent(Long ownerId, Store store) {

}
