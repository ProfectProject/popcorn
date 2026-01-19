package com.popcorn.demo.domain.store.event;

import com.popcorn.demo.domain.store.entity.Store;
import lombok.Getter;

import java.util.List;

@Getter
public class StoreGetByOwnerId {

    private final Long ownerId;
    private final List<Store> stores;

    public StoreGetByOwnerId(Long ownerId, List<Store> stores) {
        this.ownerId = ownerId;
        this.stores = stores;
    }

}
