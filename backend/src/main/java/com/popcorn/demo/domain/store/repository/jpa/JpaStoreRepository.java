package com.popcorn.demo.domain.store.repository.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.popcorn.demo.domain.store.entity.Store;

public interface JpaStoreRepository extends JpaRepository<Store, UUID> {

    @Query("SELECT s FROM Store s WHERE s.name = :name")
    Optional<Store> findByName(@Param("name") String name);

}