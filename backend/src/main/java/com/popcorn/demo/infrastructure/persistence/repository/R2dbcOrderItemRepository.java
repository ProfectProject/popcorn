package com.popcorn.demo.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.popcorn.demo.domain.order.entity.OrderItem;

public interface R2dbcOrderItemRepository extends ReactiveCrudRepository<OrderItem, UUID> {
}
