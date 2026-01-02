package com.popcorn.demo.domain.store.repository.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.store.entity.Store;

public interface JpaStoreRepository extends JpaRepository<Store, UUID> {
    
}